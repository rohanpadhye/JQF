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

import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.berkeley.cs.jqf.instrument.InstrumentingClassLoader;
import org.junit.runner.Result;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.ArrayList;

/**
 * CLI for Zest based guidance.
 *
 * @author Yevgeny Pats
 */
@CommandLine.Command(name = "ZestCLI", mixinStandardHelpOptions = true, version = "1.3")
public class ZestCLI implements Runnable{
    @Option(names = { "-e", "--exit-on-crash" },
            description = "Exit fuzzer on first crash (default: false)")
    private boolean exitOnCrash = false;

    @Option(names = { "-l", "--libfuzzer-compat-output" },
            description = "Use libFuzzer compat output instead of AFL like stats screen (default: false)")
    private boolean libFuzzerCompatOutput = false;

    @Option(names = { "-i", "--input" },
            description = "Input directory containing seed test cases (default: none)")
    private File inputDirectory;

    @Option(names = { "-o", "--output" },
            description = "Output Directory containing results (default: fuzz_results)")
    private File outputDirectory = new File("fuzz-results");

    @Parameters(index="0", paramLabel = "PACKAGE", description = "package containing the fuzzer and all dependencies")
    private String testPackageName;

    @Parameters(index="1", paramLabel = "TEST_CLASS", description = "full class name where the fuzz function is located")
    private String testClassName;

    @Parameters(index="2", paramLabel = "TEST_METHOD", description = "fuzz function name")
    private String testMethodName;


    private File[] readSeedFiles() {
        if (this.inputDirectory == null) {
            return new File[0];
        }

        ArrayList<File> seedFilesArray = new ArrayList<>();
        File[] allFiles = this.inputDirectory.listFiles();
        for (int i = 0; i < allFiles.length; i++) {
            if (allFiles[i].isFile()) {
                seedFilesArray.add(allFiles[i]);
            }
        }
        File[] seedFiles = seedFilesArray.toArray(new File[seedFilesArray.size()]);
        return seedFiles;
    }

    public void run() {

        File[] seedFiles = readSeedFiles();

        if (this.exitOnCrash) {
            System.setProperty("jqf.ei.EXIT_ON_CRASH", "true");
        }

        if (this.libFuzzerCompatOutput) {
            System.setProperty("jqf.ei.LIBFUZZER_COMPAT_OUTPUT", "true");
        }

        try {
            ClassLoader loader = new InstrumentingClassLoader(
                    new String[]{System.getProperty("java.class.path"), this.testPackageName},
                    ZestCLI.class.getClassLoader());

            // Load the guidance
            String title = this.testClassName+"#"+this.testMethodName;
            ZestGuidance guidance = seedFiles.length > 0 ?
                    new ZestGuidance(title, null, this.outputDirectory, seedFiles) :
                    new ZestGuidance(title, null, this.outputDirectory);

            // Run the Junit test
            Result res = GuidedFuzzing.run(testClassName, testMethodName, loader, guidance, System.out);
            if (Boolean.getBoolean("jqf.logCoverage")) {
                System.out.println(String.format("Covered %d edges.",
                        guidance.getTotalCoverage().getNonZeroCount()));
            }
            if (Boolean.getBoolean("jqf.ei.EXIT_ON_CRASH") && !res.wasSuccessful()) {
                System.exit(3);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }

    }
    public static void main(String[] args) {
        int exitCode = new CommandLine(new ZestCLI()).execute(args);
        System.exit(exitCode);
    }
}
