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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.function.Consumer;

import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.util.Coverage;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;

/**
 * @author Rohan Padhye
 */
public class ReproServerGuidance implements Guidance {

    protected File coverageFile;
    protected BufferedReader inputFileNameReader;
    protected File inputFile;
    protected InputStream inputFileStream;
    protected Coverage coverage = new Coverage();


    public ReproServerGuidance(String inPipe, String coverageFile) throws IOException {
        this.coverageFile = new File(coverageFile);
        this.inputFileNameReader = new BufferedReader(new InputStreamReader(new FileInputStream(inPipe)));
    }

    @Override
    public boolean hasInput() {
        // Read an input file to repro from the command pipe
        try {
            String inputFileName = inputFileNameReader.readLine();
            if (inputFileName == null) {
                throw new IOException("End of input stream");
            }
            this.inputFile = new File(inputFileName);

        } catch (IOException e) {
            return false;
        }

        // Continue unless stopped (which would cause I/O errors)
        return true;
    }

    @Override
    public InputStream getInput() throws IllegalStateException, GuidanceException {
        // Clear coverage stats for this run
        coverage.clear();

        // Read input bytes from the specified file
        try {
            return (this.inputFileStream = new BufferedInputStream(new FileInputStream(this.inputFile)));
        } catch (IOException e) {
            throw new GuidanceException(e);
        }
    }

    @Override
    public void handleResult(Result result, Throwable error){
        // Close the open input file
        try {
            if (inputFileStream != null) {
                inputFileStream.close();
            }
        } catch (IOException e) {
            throw new GuidanceException(e);
        }

        // Print coverage
        try (PrintWriter out = new PrintWriter(coverageFile)) {
            coverage.getCovered().toSortedList().forEach((i) ->
                out.println(String.format("%05d", i))
            );
            out.println(result.toString()); // EOF marker for tools to realize that repro is done
        } catch (IOException e) {
            throw new GuidanceException(e);
        }
    }

    @Override
    public Consumer<TraceEvent> generateCallBack(Thread thread) {
        return coverage::handleEvent;
    }
}
