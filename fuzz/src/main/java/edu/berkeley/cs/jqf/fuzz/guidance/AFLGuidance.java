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
import java.nio.Buffer;
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
 * <p>An instance of this class actually communicates with a proxy that
 * sits between AFL and JQF. The proxy is the target program launched by
 * AFL; it passes messages back and forth between AFL and JQF and
 * helps populate the shared memory coverage buffer that the JVM cannot
 * access.
 *
 * @author Rohan Padhye and Caroline Lemieux
 */
public class AFLGuidance implements Guidance {

    /** The file in which AFL will write its input. */
    protected final File inputFile;

    /** The communication channel from AFL proxy to us. */
    protected final InputStream proxyInput;

    /** The communication channel from us to the AFL proxy. */
    protected final OutputStream proxyOutput;

    /** The size of the "coverage" map that will be sent to AFL. */
    protected static final int COVERAGE_MAP_SIZE = 1 << 16;

    /** The "coverage" map that will be sent to AFL. */
    protected byte[] traceBits;

    /** Whether to keep executing more inputs. */
    protected boolean everything_ok = true;

    /** The bits that will be communicated to the AFL proxy. */
    protected ByteBuffer feedback;

    /** A temporary holding the opened input file stream during a run. */
    private InputStream inputFileStream;

    /**
     * A call stack to keep track of which method we are in.
     *
     * <p>Note: We assume there is only a single app thread running.
     * For supporting multiple threads, we would have to store
     * a map from threads to call stacks.
     */
    private Deque<CallEvent> callStack = new ArrayDeque<>();

    /**
     * Whether the above call stack is empty.
     *
     * <p>We use a separate
     * volatile field rather than rely on callStack.isEmpty()
     * returning a synced value, since the stack is manipulated by
     * the app thread(s).
     *
     * <p>Note: Same assumption on single-threaded app applies.
     */
    private volatile boolean callStackEmpty = true;

    private static final int FEEDBACK_BUFFER_SIZE = 1 << 17;
    private static final byte[] FEEDBACK_ZEROS = new byte[FEEDBACK_BUFFER_SIZE];

    /**
     * Creates an instance of an AFLGuidance given file handles for I/O.
     *
     * @param inputFile  the file that AFL will write inputs to
     * @param inPipe     a FIFO-like pipe for receiving messages from the AFL proxy
     * @param outPipe    a FIFO-like pipe for sending messages to the AFL proxy
     * @throws IOException  if any file or pipe could not be opened
     */
    public AFLGuidance(File inputFile, File inPipe, File outPipe) throws IOException {
        this.inputFile = inputFile;
        this.proxyInput = new BufferedInputStream(new FileInputStream(inPipe));
        this.proxyOutput = new BufferedOutputStream(new FileOutputStream(outPipe));
        this.feedback = ByteBuffer.allocate(FEEDBACK_BUFFER_SIZE);
        this.feedback.order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Creates an instance of an AFLGuidance given file names for I/O.
     *
     * @param inputFileName  the file that AFL will write inputs to
     * @param inPipeName     a FIFO-like pipe for receiving messages from the AFL proxy
     * @param outPipeName    a FIFO-like pipe for sending messages to the AFL proxy
     * @throws IOException  if any file or pipe could not be opened
     */
    public AFLGuidance(String inputFileName, String inPipeName, String outPipeName) throws IOException {
        this(new File(inputFileName), new File(inPipeName), new File(outPipeName));
    }

    /**
     * Closes the pipes used to communicate with the AFL proxy.
     */
    @Override
    public void finalize() {
        if (proxyInput != null) {
            try {
                proxyInput.close();
            } catch (IOException e) {
                // Ignore
            }
        }

        if (proxyOutput != null) {
            try {
                proxyOutput.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }


    /**
     * Returns an input stream containing the bytes that AFL
     * has written to.
     *
     * @return  a stream of bytes to be used by the input generator(s)
     * @throws IllegalStateException if the last {@link #hasInput()}
     *                  returned <tt>false</tt>
     * @throws GuidanceException if there was an I/O error when opening the file
     */
    @Override
    public InputStream getInput() throws IllegalStateException, GuidanceException {
        // Sanity check
        assert(callStackEmpty);

        // Should not be here if hasInput() returned false
        if (!everything_ok) {
            throw new IllegalStateException("Fuzzing should have been stopped.");
        }

        try {
            this.inputFileStream = new BufferedInputStream(new FileInputStream(this.inputFile));
            return this.inputFileStream;
        } catch (IOException e) {
            throw new GuidanceException(e);
        }
    }

    /**
     * Waits for the AFL proxy to send a ready signal.
     *
     * @return Returns <tt>true</tt> in the absence of I/O errors
     */
    @Override
    public boolean hasInput() {

        if (everything_ok) {
            // Get a 4-byte signal from AFL
            byte[] signal = new byte[4];
            try {
                int received = proxyInput.read(signal, 0, 4);
                if (received != 4) {
                    throw new IOException("Could not read `ready` from AFL");
                }

                // Reset trace-bits
                traceBits = new byte[COVERAGE_MAP_SIZE];

            } catch (IOException e) {
                everything_ok = false;
            }
        }

        // Continue unless stopped (which would cause I/O errors)
        return everything_ok;
    }

    /**
     * Notifies the AFL proxy that a run has completed and whether
     * it was a success. 1
     *
     * <p>This method also sends coverage information back to the AFL
     * proxy, which is responsible for updating the shared memory
     * region used by afl-fuzz.
     *
     * <p>If the trial resulted in an assumption violation, we do not
     * mark it is a crash, but we also do not send any coverage feedback
     * so that AFL does not consider the last input interesting enough to
     * keep in its queue.
     *
     * @param result    the result of the fuzzing trial
     * @param error     the exception thrown by the test, or <tt>null</tt>
     */
    @Override
    public void handleResult(Result result, Throwable error) {
        // Wait for all events to be handled by the app thread
        while(!callStackEmpty);

        // Close the open input file
        try {
            if (inputFileStream != null) {
                inputFileStream.close();
            }
        } catch (IOException e) {
            throw new GuidanceException(e);
        }

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
                for (int i = 1; i < COVERAGE_MAP_SIZE; i++) {
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
            proxyOutput.write(feedback.array(), 0, feedback.position());
            proxyOutput.flush();
        } catch (IOException e) {
            everything_ok = false;
        }

    }

    /**
     * Returns a callback to handle trace events
     *
     * <p>For the main thread, this returns a reference to
     * {@link #handleEvent(TraceEvent)}. For other threads,
     * this returns a callback that does nothing, since AFL
     * is not equipped to handle multi-threaded applications.</p>
     *
     * @param thread the thread whose events to handle
     * @return a callback to handle trace events
     */
    public Consumer<TraceEvent> generateCallBack(Thread thread) {
        if (thread.getName().equals("main")) {
            return this::handleEvent;
        } else {
            return (e) -> { /* Ignore */ };
        }
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
            // Map branch IID to [1, MAP_SIZE); the odd bound also reduces collisions
            int edgeId = 1 + Hashing.hash1(b.getIid(), b.getArm(), COVERAGE_MAP_SIZE-1);

            // Increment the 8-bit branch counter
            incrementTraceBits(edgeId);
        } else if (e instanceof CallEvent) {
            // Add a call event to the stack
            callStack.push((CallEvent) e);
            // Mark the volatile indicator to non-empty
            if (callStackEmpty) {
                callStackEmpty = false;
            }

            // Map IID to [1, MAP_SIZE]; the odd bound also reduces collisions
            int edgeId = 1 + Hashing.hash(e.getIid(), COVERAGE_MAP_SIZE-1);

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
     * <p>Overflows are possible but ignored (as in AFL).
     *
     * @param index the key in the trace bits map
     */
    protected void incrementTraceBits(int index) {
        traceBits[index]++;
    }


    /** Clears the feedback buffer by resetting it to zero. */
    protected void clearFeedbackBuffer() {
        // These redundant casts are to prevent Java 9's covariant
        // return types to use the new methods that return ByteBuffer
        // instead, which do not exist in JDK 8 and below.
        ((Buffer) feedback).rewind();
        feedback.put(FEEDBACK_ZEROS);
        ((Buffer) feedback).rewind();
    }


}