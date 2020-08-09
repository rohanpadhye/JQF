/*
 * Copyright (c) 2020, The Regents of the University of California
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
package edu.berkeley.cs.jqf.fuzz.repro;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import edu.berkeley.cs.jqf.fuzz.afl.AFLGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;

/**
 * @author Rohan Padhye
 */
public class ReproServerGuidance extends AFLGuidance {

    protected File coverageFile;
    protected BufferedReader inputFileReader;


    public ReproServerGuidance(String inPipe, String coverageFile) throws IOException {
        super("/dev/null", inPipe, "/dev/null");
        this.coverageFile = new File(coverageFile);
        this.inputFileReader = new BufferedReader(new InputStreamReader(this.proxyInput));
    }

    @Override
    public boolean hasInput() {
        if (everything_ok) {
            try {
                String inputFileName = inputFileReader.readLine();
                if (inputFileName == null) {
                    throw new IOException("End of input stream");
                }
                this.inputFile = new File(inputFileName);

                // Reset trace-bits
                traceBits = new byte[COVERAGE_MAP_SIZE];

            } catch (IOException e) {
                everything_ok = false;
            }
        }

        // Continue unless stopped (which would cause I/O errors)
        return everything_ok;
    }

    @Override
    public void handleResult(Result result, Throwable error){
        super.handleResult(result, error);

        try (PrintWriter out = new PrintWriter(coverageFile)) {
            for (int i = 0; i < traceBits.length; i++) {
                if (traceBits[i] != 0) {
                    out.println(i);
                }
            }
            out.println(traceBits.length); // EOF marker for tools to realize that repro is done
        } catch (IOException e) {
            throw new GuidanceException(e);
        }
    }


}
