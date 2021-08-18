/*
 * Copyright (c) 2017-2018 The Regents of the University of California
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

package edu.berkeley.cs.jqf.fuzz.ei;

import java.io.File;

import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;

/**
 * Entry point for fuzzing with Execution Indexing.
 *
 * @author Rohan Padhye
 */
public class ExecutionIndexingDriver {

    public static void main(String[] args) {
        if (args.length < 2){
            System.err.println("Usage: java " + ExecutionIndexingDriver.class + " TEST_CLASS TEST_METHOD [OUTPUT_DIR [SEEDS...]]");
            System.exit(1);
        }

        String testClassName  = args[0];
        String testMethodName = args[1];
        String outputDirectoryName = args.length > 2 ? args[2] : "fuzz-results";
        File outputDirectory = new File(outputDirectoryName);
        File[] seedFiles = null;
        if (args.length > 3) {
            seedFiles = new File[args.length-3];
            for (int i = 3; i < args.length; i++) {
                seedFiles[i-3] = new File(args[i]);
            }
        }

        try {
            // Load the guidance
            String title = testClassName+"#"+testMethodName;
            ZestGuidance guidance = seedFiles != null ?
                    new ExecutionIndexingGuidance(title, null, outputDirectory, seedFiles) :
                    new ExecutionIndexingGuidance(title, null, outputDirectory, new File[]{});

            // Ensure that generators are being traced
            System.setProperty("jqf.traceGenerators", "true");

            // Run the Junit test
            GuidedFuzzing.run(testClassName, testMethodName, guidance, System.out);
            if (Boolean.getBoolean("jqf.logCoverage")) {
                System.out.println(String.format("Covered %d edges.",
                        guidance.getTotalCoverage().getNonZeroCount()));
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }

    }
}
