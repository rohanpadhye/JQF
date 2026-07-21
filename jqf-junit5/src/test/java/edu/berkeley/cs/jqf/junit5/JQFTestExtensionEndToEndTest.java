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
package edu.berkeley.cs.jqf.junit5;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGenerator;
import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGeneratorFactory;
import edu.berkeley.cs.jqf.fuzz.spi.SkipTrialException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * End-to-end tests that drive a real {@code @FuzzTest} method through the standard
 * {@code junit-jupiter-engine} and {@link JQFTestExtension}, using the JUnit
 * Platform {@link Launcher}.
 *
 * <p>The sample below carries its own tiny {@link ArgumentsGeneratorFactory}, so
 * this module needs no generator provider on its classpath (keeping it free of
 * junit-quickcheck). The campaign here is uninstrumented, so the planted bug must
 * be reachable by ordinary random search; a real coverage-guided campaign on
 * instrumented code is exercised by the integration tests.
 */
class JQFTestExtensionEndToEndTest {

    @BeforeEach
    @AfterEach
    void resetState() {
        JUnit5FuzzConfig.clearInstalledGuidance();
        System.clearProperty("jqf.fuzz");
        System.clearProperty("jqf.fuzz.trials");
        System.clearProperty("jqf.fuzz.out");
    }

    @Test
    void fuzzingModeFindsThePlantedBug() throws IOException {
        assumeTrue(System.getenv("JQF_FUZZ") == null);
        Path out = Files.createTempDirectory("jqf-junit5-fuzz");
        System.setProperty("jqf.fuzz", "true");
        System.setProperty("jqf.fuzz.trials", "2000");
        System.setProperty("jqf.fuzz.out", out.toString());

        TestExecutionSummary summary = run(PlantedBugScenario.class);

        // The single @FuzzTest node fails because fuzzing reaches a negative input.
        assertEquals(1, summary.getTestsFailedCount(),
                "fuzzing should fail the @FuzzTest node by finding a negative input");
    }

    @Test
    void regressionModeIsGreenAndBounded() throws IOException {
        assumeTrue(System.getenv("JQF_FUZZ") == null);
        // A fresh, empty output directory means no saved corpus: only the empty input runs.
        Path out = Files.createTempDirectory("jqf-junit5-regression");
        System.setProperty("jqf.fuzz.out", out.toString());

        TestExecutionSummary summary = run(PlantedBugScenario.class);

        assertEquals(0, summary.getTestsFailedCount(), "regression over the empty input must not hit the bug");
        // Exactly one bounded invocation (the empty input), not a campaign.
        assertEquals(1, summary.getTestsSucceededCount(), "regression should run exactly the one empty input");
    }

    @Test
    void regressionSkipsTheNodeWhenNoInputCanGenerateObjectArguments() throws IOException {
        assumeTrue(System.getenv("JQF_FUZZ") == null);
        // No corpus, and the generator produces arguments for no input. The node must be skipped,
        // not run with a null object default that the body would dereference into an NPE.
        Path out = Files.createTempDirectory("jqf-junit5-skip");
        System.setProperty("jqf.fuzz.out", out.toString());

        TestExecutionSummary summary = run(UngeneratableScenario.class);

        assertEquals(0, summary.getTestsFailedCount(),
                "an un-generatable object argument must skip the node, not fail it with a null");
        assertEquals(0, summary.getTestsSucceededCount(),
                "no input yields arguments, so the node runs no invocation");
    }

    private static TestExecutionSummary run(Class<?> testClass) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(testClass))
                .build();
        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
        return listener.getSummary();
    }

    /**
     * A self-contained fuzz target with a planted bug: it throws for any negative
     * input. The empty (all-zero) regression input decodes to {@code 0}, which is
     * non-negative, so regression stays green.
     */
    static class PlantedBugScenario {
        @FuzzTest(arguments = IntFactory.class)
        void rejectsNegativeNumbers(int x) {
            assertTrue(x >= 0, "found a negative input: " + x);
        }
    }

    /**
     * A minimal generator that reads a single {@code int} from the guided byte
     * stream by wrapping it in a {@link StreamBackedRandom}.
     */
    public static final class IntFactory implements ArgumentsGeneratorFactory {
        @Override
        public ArgumentsGenerator create(Class<?> testClass, Method testMethod) {
            return input -> {
                StreamBackedRandom random = new StreamBackedRandom(input);
                return new Object[] {random.nextInt()};
            };
        }
    }

    /**
     * A fuzz target whose object argument no input can produce: the generator always skips. Without
     * the guard, the regression fallback would pass {@code null} and the body would throw a
     * {@link NullPointerException}.
     */
    static class UngeneratableScenario {
        @FuzzTest(arguments = SkippingFactory.class)
        void requiresAnObject(Object value) {
            value.toString();
        }
    }

    /** A generator that never produces arguments; every input is skipped. */
    public static final class SkippingFactory implements ArgumentsGeneratorFactory {
        @Override
        public ArgumentsGenerator create(Class<?> testClass, Method testMethod) {
            return input -> {
                throw new SkipTrialException();
            };
        }
    }
}
