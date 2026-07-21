/*
 * Copyright (c) 2026 Vladimir Sitnikov and JQF Contributors
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
package edu.berkeley.cs.jqf.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.repro.ReproGuidance;
import edu.berkeley.cs.jqf.instrument.InstrumentingClassLoader;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Drives {@link FuzzTestDispatcher} against the instrumented example classes, the
 * same way the {@code jqf:fuzz} and {@code jqf:repro} goals do.
 *
 * <p>It checks that the plugin detects the JUnit 4 and JUnit 5 styles, fuzzes a
 * JUnit 5 {@code @FuzzTest} until it finds a planted bug, replays the saved input,
 * and still runs a JUnit 4 {@code @Fuzz} campaign unchanged. The example classes
 * are loaded from {@code ../examples/target} through an
 * {@link InstrumentingClassLoader}, so they carry real coverage instrumentation
 * and are not shadowed by this module's own classpath.
 */
public class FuzzTestDispatcherIT {

    private static final String SIMPLE_JUNIT4 = "edu.berkeley.cs.jqf.examples.simple.SimpleClassTest";
    private static final String SIMPLE_JUNIT5 = "edu.berkeley.cs.jqf.examples.junit5.SimpleFuzzTest";
    private static final String PLANTED_BUG_JUNIT5 = "edu.berkeley.cs.jqf.examples.junit5.PlantedBugFuzzTest";

    private File resultsDir;
    private ClassLoader classLoader;

    @Before
    public void setUp() throws IOException {
        Path examplesTarget = Paths.get("../examples/target");
        Assume.assumeTrue("examples must be built (test-classes + copied dependencies) before this IT runs",
                Files.isDirectory(examplesTarget.resolve("test-classes"))
                        && Files.isDirectory(examplesTarget.resolve("dependency")));

        resultsDir = Files.createTempDirectory("jqf-plugin-it").toFile();

        // Walk the copied dependency tree of jqf-examples plus its own output, exactly
        // as the integration-tests harness does.
        List<String> paths = Files.walk(examplesTarget.resolve("dependency"))
                .map(Path::toString).collect(Collectors.toList());
        paths.add("../examples/target/classes/");
        paths.add("../examples/target/test-classes/");
        classLoader = new InstrumentingClassLoader(paths.toArray(new String[0]), getClass().getClassLoader());
    }

    @Test
    public void detectsJUnit5FuzzTest() throws Exception {
        Class<?> testClass = classLoader.loadClass(SIMPLE_JUNIT5);
        Assert.assertEquals(FuzzTestDispatcher.TestFramework.JUNIT5,
                FuzzTestDispatcher.detect(testClass, "testWithGenerator"));
    }

    @Test
    public void detectsJUnit4Fuzz() throws Exception {
        Class<?> testClass = classLoader.loadClass(SIMPLE_JUNIT4);
        Assert.assertEquals(FuzzTestDispatcher.TestFramework.JUNIT4,
                FuzzTestDispatcher.detect(testClass, "testWithGenerator"));
    }

    @Test
    public void junit4PathRunsUnchanged() throws Exception {
        ZestGuidance zest = new ZestGuidance("SimpleClassTest", null, 5000L, resultsDir, new Random(42));

        FuzzTestDispatcher.Outcome outcome =
                FuzzTestDispatcher.run(classLoader, SIMPLE_JUNIT4, "testWithGenerator", zest, null);

        Assert.assertTrue("a clean JUnit 4 target should report no failures", outcome.wasSuccessful());
        // The same seven branches the JUnit 4 path explores in ZestGuidanceIT.
        Assert.assertEquals(7, zest.getTotalCoverage().getNonZeroCount());
    }

    @Test
    public void junit5FuzzFindsPlantedBugThenReproReplays() throws Exception {
        // Fuzzing: a coverage-guided campaign must reach a negative input.
        ZestGuidance zest = new ZestGuidance("PlantedBugFuzzTest", null, 1000L, resultsDir, new Random(42));

        FuzzTestDispatcher.Outcome fuzzOutcome =
                FuzzTestDispatcher.run(classLoader, PLANTED_BUG_JUNIT5, "mustBeNonNegative", zest, null);

        Assert.assertFalse("fuzzing should find the planted negative input", fuzzOutcome.wasSuccessful());

        // The failing input is saved so it can be replayed.
        File[] failures = new File(resultsDir, "failures").listFiles(File::isFile);
        Assert.assertNotNull("a failures directory should exist", failures);
        Assert.assertTrue("a failing input must be saved for repro", failures.length > 0);

        // Repro: replaying the saved input through the same dispatch reproduces the failure.
        ReproGuidance repro = new ReproGuidance(failures[0], null);
        FuzzTestDispatcher.Outcome reproOutcome =
                FuzzTestDispatcher.run(classLoader, PLANTED_BUG_JUNIT5, "mustBeNonNegative", repro, null);

        Assert.assertFalse("repro should reproduce the saved failure", reproOutcome.wasSuccessful());
    }
}
