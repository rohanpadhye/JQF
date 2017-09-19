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

package jwig.fuzz.guidance;

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

import jwig.logging.SingleSnoop;
import jwig.logging.events.BranchEvent;
import jwig.logging.events.TraceEvent;


/**
 * @author Rohan Padhye and Caroline Lemieux
 */
public class AFLGuidance implements Guidance {

    private File inputFile;
    private InputStream in;
    private OutputStream out;
    ByteBuffer feedback;
    private static final int COVERAGE_MAP_SIZE = 1 << 16;
    private byte[] traceBits;

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


    @Override
    public File inputFile() {
        return this.inputFile;
    }

    @Override
    public boolean waitForInput() throws IOException {
        // Get a 4-byte signal from AFL
        byte[] signal = new byte[4];
        int received = in.read(signal, 0, 4);
        if (received != 4) {
            throw new IOException("Did not receive `ready` from AFL");
        }

        // Reset trace-bits
        traceBits = new byte[COVERAGE_MAP_SIZE];

        // Always produce new input (AFL can only be stopped abruptly)
        return true;
    }

    @Override
    public void notifyEndOfRun(boolean success, Throwable error) throws IOException {
        // Wait for instrumentation to process this thread's instructions
        SingleSnoop.waitForQuiescence();

        // Reset the feedback buffer for a new run
        feedback.clear();

        // Put the return status into the feedback buffer
        int status = error == null ? 0 : 1;
        feedback.putInt(status);

        // Put AFL's trace_bits map into the feedback buffer
        for (int i = 0; i < COVERAGE_MAP_SIZE; i++) {
            feedback.put(traceBits[i]);
        }

        // Send feedback to AFL
        out.write(feedback.array(), 0, feedback.position());
        out.flush();

    }

    public Consumer<TraceEvent> generateCallBack(String threadName) {
        return this::handleEvent;
    }

    private void handleEvent(TraceEvent e) {
        if (e instanceof BranchEvent) {
            BranchEvent b = (BranchEvent) e;
            int edgeId = b.getIid() % COVERAGE_MAP_SIZE;
            if (b.isTaken()) {
                edgeId = COVERAGE_MAP_SIZE - edgeId;
            }
            traceBits[edgeId]++;
        }
    }

}