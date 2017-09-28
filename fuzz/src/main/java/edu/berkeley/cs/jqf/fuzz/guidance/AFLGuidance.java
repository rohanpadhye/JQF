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
import java.util.function.Consumer;

import edu.berkeley.cs.jqf.instrument.tracing.events.BranchEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;

import static edu.berkeley.cs.jqf.fuzz.guidance.Result.ASSUMPTION_VIOLATED;
import static edu.berkeley.cs.jqf.fuzz.guidance.Result.FAILURE;


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

    protected File inputFile;
    protected InputStream in;
    protected OutputStream out;
    protected static final int COVERAGE_MAP_SIZE = 1 << 16;
    protected byte[] traceBits;
    protected boolean everything_ok = true;

    private ByteBuffer feedback;

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

        // Always produce new input (AFL can only be stopped abruptly)
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
     * @param result    the result of the fuzzing tiral
     * @param error     the exception thrown by the test, or <tt>null</tt>
     * @throws IOException
     */
    @Override
    public void handleResult(Result result, Throwable error) {
        // Reset the feedback buffer for a new run
        feedback.clear();

        // Put the return status into the feedback buffer
        // --> Return status is 1 if and only if an error occurs
        int status = result == FAILURE ? 1 : 0;
        feedback.putInt(status);

        // Put AFL's trace_bits map into the feedback buffer
        // --> Skip this step if an assumption was violated
        if (result != ASSUMPTION_VIOLATED) {
            for (int i = 0; i < COVERAGE_MAP_SIZE; i++) {
                feedback.put(traceBits[i]);
            }
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
            int edgeId = iidToEdgeIdx(b.getIid(), COVERAGE_MAP_SIZE);

            // Take complement for reverse branches
            if (b.isTaken()) {
                edgeId = COVERAGE_MAP_SIZE - edgeId;
            }

            // Increment the 8-bit branch counter
            incrementTraceBits(edgeId);
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


    /**
     * Converts a Janala-generated instruction identifier to
     * a pseudo-uniformly distributed edge index
     *
     * @param iid   the Janala-generated instruction ID
     * @param bound the upper bound (exclusive) on the edge ID
     * @return      a value in [0, MAP_SIZE)
     */
    protected static int iidToEdgeIdx(int iid, int bound) {
        int hash = (int)((iid * 0x5DEECE66DL + 0xBL) >> 32);
        int edgeId = hash % bound;
        if (edgeId < 0) {
            edgeId += bound;
        }
        assert(edgeId >= 0 && edgeId < bound);
        return edgeId;
    }

}