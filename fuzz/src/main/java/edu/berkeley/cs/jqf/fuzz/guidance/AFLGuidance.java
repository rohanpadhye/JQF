/*
 * Copyright (c) 2017, University of California, Berkeley
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.berkeley.cs.jqf.fuzz.guidance;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

import edu.berkeley.cs.jqf.fuzz.util.Hashing;
import edu.berkeley.cs.jqf.instrument.tracing.events.BranchEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReturnEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;


/**
 * A front-end that uses AFL for guided fuzzing.
 *
 * An instance of this class actually communicates with a proxy that
 * sits between AFL and JQF. The proxy is the target program launched by
 * AFL; it passes messages back and forth between AFL and JQF and
 * helps populate the shared memory coverage buffer that the JVM cannot
 * access.
 *
 * @author Rohan Padhye and Caroline Lemieux
 */
public class AFLGuidance implements Guidance {

    /** The file in which AFL will write its input. */
    protected File inputFile;

    /** The communication channel from AFL proxy to us. */
    protected InputStream in;

    /** The communication channel from us to the AFL proxy. */
    protected OutputStream out;

    /** The size of the "coverage" map that will be sent to AFL. */
    protected static final int COVERAGE_MAP_SIZE = 1 << 16;

    /** The "coverage" map that will be sent to AFL. */
    protected byte[] traceBits;

    /** Whether to keep executing more inputs. */
    protected boolean everything_ok = true;

    /** The bits that will be communicated to the AFL proxy. */
    protected ByteBuffer feedback;

    /**
     * A call stack to keep track of which method we are in.
     *
     * Note: We assume there is only a single app thread running.
     * For supporting multiple threads, we would have to store
     * a map from threads to call stacks.
     */
    private Deque<CallEvent> callStack = new ArrayDeque<>();

    /**
     * Whether the above call stack is empty. We use a separate
     * volatile field rather than rely on callStack.isEmpty()
     * returning a synced value, since the stack is manipulated by
     * the app thread(s).
     *
     * Note: Same assumption on single-threaded app applies.
     */
    private volatile boolean callStackEmpty = true;

    /**
     *
     * @param inputFile  the file that AFL will write inputs to
     * @param inPipe     a FIFO-like pipe for receiving messages from the AFL proxy
     * @param outPipe    a FIFO-like pipe for sending messages to the AFL proxy
     * @throws IOException  if any file or pipe could not be opened
     */
    public AFLGuidance(File inputFile, File inPipe, File outPipe) throws IOException {
        this.inputFile = inputFile;
        this.in = new BufferedInputStream(new FileInputStream(inPipe));
        this.out = new BufferedOutputStream(new FileOutputStream(outPipe));
        this.feedback = ByteBuffer.allocate(1 << 20);
        this.feedback.order(ByteOrder.LITTLE_ENDIAN);
    }

    public AFLGuidance(String inputFileName, String inPipeName, String outPipeName) throws IOException {
        this(new File(inputFileName), new File(inPipeName), new File(outPipeName));
    }

    /**
     * Closes the pipes used to communicate with the AFL proxy.
     */
    @Override
    public void finalize() {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                // Ignore
            }
            in = null;
        }

        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                // Ignore
            }
            out = null;
        }
    }


    /**
     * Returns a handle to the file that AFL will write inputs to.
     *
     * @return the file handle
     */
    @Override
    public File getInputFile() {
        return this.inputFile;
    }

    /**
     * Waits for the AFL proxy to send a ready signal.
     *
     * @return Always returns <tt>true</tt>.
     * @throws IOException  if the ready signal cannot be read
     */
    @Override
    public boolean hasInput() {
        // Sanity check
        assert callStackEmpty;

        // Get a 4-byte signal from AFL
        byte[] signal = new byte[4];
        try {
            int received = in.read(signal, 0, 4);
            if (received != 4) {
                throw new IOException("Could not read `ready` from AFL");
            }
        } catch (IOException e) {
            everything_ok = false;
        }

        // Reset trace-bits
        traceBits = new byte[COVERAGE_MAP_SIZE];

        // Continue unless stopped
        return everything_ok;
    }

    /**
     * Notifies the AFL proxy that a run has completed and whether
     * it was a success.
     *
     * This method also sends coverage information back to the AFL
     * proxy, which is responsible for updating the shared memory
     * region used by afl-fuzz.
     *
     * If the trial resulted in an assumption violation, we do not
     * mark it is a crash, but we also do not send any coverage feedback
     * so that AFL does not consider the last input interesting enough to
     * keep in its queue.
     *
     * @param result    the result of the fuzzing trial
     * @param error     the exception thrown by the test, or <tt>null</tt>
     * @throws IOException
     */
    @Override
    public void handleResult(Result result, Throwable error) {
        // Wait for all events to be handled by the app thread
        while(!callStackEmpty);

        // Reset the feedback buffer for a new run
        clearFeedbackBuffer();

        // Set at least one tracebit so that AFL doesn't complain about
        // no instrumentation
        traceBits[0] = traceBits[0] == 0 ? 1 : traceBits[0];


        // Check result and set status value
        int status;
        switch (result) {
            case SUCCESS: {
                // For success, the exit value is zero
                status = 0;
                break;
            }
            case FAILURE: {
                // For failure, the exit value is non-zero
                status = 1;
                break;
            }
            case INVALID: {
                // For invalid inputs, we don't send a non-zero
                // status to prevent AFL from marking this as a "crash"
                status = 0;

                // However, we clear trace-bits so that AFL does not
                // ever consider such an input as interesting enough to
                // save in its queue
                for (int i = 0; i < COVERAGE_MAP_SIZE; i++) {
                    traceBits[i] = 0;
                }
                break;
            }
            default: {
                throw new IllegalArgumentException("Invalid result: " + result);
            }
        }

        // Send the status value to AFL
        feedback.putInt(status);

        // Send trace-bits to AFL as a contiguous array
        for (int i = 0; i < COVERAGE_MAP_SIZE; i++) {
            feedback.put(traceBits[i]);
        }


        // Send feedback to AFL
        try {
            out.write(feedback.array(), 0, feedback.position());
            out.flush();
        } catch (IOException e) {
            everything_ok = false;
        }

    }

    public Consumer<TraceEvent> generateCallBack(String threadName) {
        return this::handleEvent;
    }

    /**
     * Records branch coverage by snooping on branch events
     * and incrementing the branch-specific counter in
     * the tracebits map.
     *
     * @param e  the trace event to handle
     */
    protected void handleEvent(TraceEvent e) {
        if (e instanceof BranchEvent) {
            BranchEvent b = (BranchEvent) e;
            // Map branch IID to [0, MAP_SIZE)
            int edgeId = Hashing.hash1(b.getIid(), b.getArm(), COVERAGE_MAP_SIZE);

            // Increment the 8-bit branch counter
            incrementTraceBits(edgeId);
        } else if (e instanceof CallEvent) {
            // Add a call event to the stack
            callStack.push((CallEvent) e);
            // Mark the volatile indicator to non-empty
            if (callStackEmpty) {
                callStackEmpty = false;
            }

            // Map IID to [0, MAP_SIZE]
            int edgeId = Hashing.hash(e.getIid(), COVERAGE_MAP_SIZE);

            // Increment the 8-bit branch counter
            incrementTraceBits(edgeId);


        } else if (e instanceof ReturnEvent) {
            // Remove call event from the stack
            callStack.pop();
            // Mark the volatile indicator to empty if the stack is empty
            if (callStack.isEmpty()) {
                callStackEmpty = true;
            }
        }
    }

    /**
     * Increments the 8-bit counter at given index.
     *
     * Overflows possible.
     *
     * @param index the key in the trace bits map
     */
    protected void incrementTraceBits(int index) {
        traceBits[index]++;
    }


    /** Clears the feedback buffer by reseting it to zero. */
    protected void clearFeedbackBuffer() {
        feedback.clear();
        while (feedback.hasRemaining()) {
            feedback.put((byte) 0);
        }
        feedback.clear();
    }


}