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

package jwig.fuzz;
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
import java.util.HashMap;
import java.util.Map;
import com.pholser.junit.quickcheck.guided.Guidance;


/**
 * @author Rohan Padhye and Caroline Lemieux
 */
public class AFLGuidance implements Guidance {

    private File inputFile;
    private InputStream in;
    private OutputStream out;
    ByteBuffer feedback;
    private static final int COVERAGE_MAP_SIZE = 1 << 16;

    public AFLGuidance(File inputFile, File inPipe, File outPipe) throws IOException {
        this.inputFile = inputFile;
        this.in = new BufferedInputStream(new FileInputStream(inPipe));
        this.out = new BufferedOutputStream(new FileOutputStream(outPipe));
        this.feedback = ByteBuffer.allocate(1 << 20);
        this.feedback.order(ByteOrder.LITTLE_ENDIAN);
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
    public void waitForInput() throws IOException {
        // Get a 4-byte signal from AFL
        byte[] signal = new byte[4];
        System.out.println("Waiting for input...");
        int received = in.read(signal, 0, 4);
        if (received != 4) {
            throw new RuntimeException(String.format("Received" +
                    " only %d bytes from AFL proxy; expecting 4", received));
        }
    }

    @Override
    public void notifyEndOfRun(boolean success) throws IOException {
        // Reset the feedback buffer for a new run
        feedback.clear();

        // Put the return status into the feedback buffer
        int status = success ? 0 : 1;
        feedback.putInt(status);

        // @TODO: Get this from the coverage logger
        Map<Integer, Byte> traceBits = new HashMap<>();

        // Put AFL's trace_bits map into the feedback buffer
        for (int i = 0; i < COVERAGE_MAP_SIZE; i++) {
            //byte count = traceBits.containsKey(i) ?
            //        traceBits.get(i) : 0;
            byte count = 1;
            //feedback.put(count);
        }

        // Send feedback to AFL
        out.write(feedback.array(), 0, feedback.position());
        out.flush();



    }

}