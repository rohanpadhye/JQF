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

import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.function.Consumer;

import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Unit tests for the run-mode selection and configuration parsing in
 * {@link JUnit5FuzzConfig}. These run with no argument-generator provider on the
 * classpath, so they exercise only the configuration logic, not a real campaign.
 */
class JUnit5FuzzConfigTest {

    /** Holder for sample {@code @FuzzTest} methods reflected by the tests. */
    static class Samples {
        @FuzzTest
        void plain(int x) {
        }

        @FuzzTest(repro = "saved/input")
        void withRepro(int x) {
        }

        @FuzzTest(maxTrials = 1234)
        void withTrials(int x) {
        }

        @FuzzTest(maxDuration = "5m")
        void withDuration(int x) {
        }
    }

    private static Method sample(String name) throws NoSuchMethodException {
        return Samples.class.getDeclaredMethod(name, int.class);
    }

    private static JUnit5FuzzConfig configFor(String methodName) throws NoSuchMethodException {
        return JUnit5FuzzConfig.from(Samples.class, sample(methodName));
    }

    @BeforeEach
    @AfterEach
    void resetState() {
        JUnit5FuzzConfig.clearInstalledGuidance();
        System.clearProperty("jqf.fuzz");
        System.clearProperty("jqf.repro");
        System.clearProperty("jqf.fuzz.trials");
        System.clearProperty("jqf.fuzz.duration");
    }

    @Test
    void defaultsToRegressionMode() throws NoSuchMethodException {
        // Only meaningful when the environment is not forcing fuzzing.
        assumeTrue(System.getenv("JQF_FUZZ") == null);
        assertFalse(configFor("plain").isFuzzingMode());
    }

    @Test
    void systemPropertySelectsFuzzingMode() throws NoSuchMethodException {
        System.setProperty("jqf.fuzz", "true");
        assertTrue(configFor("plain").isFuzzingMode());
    }

    @Test
    void reproAttributeSelectsFuzzingMode() throws NoSuchMethodException {
        assumeTrue(System.getenv("JQF_FUZZ") == null);
        assertTrue(configFor("withRepro").isFuzzingMode());
    }

    @Test
    void installedGuidanceSelectsFuzzingMode() throws NoSuchMethodException {
        assumeTrue(System.getenv("JQF_FUZZ") == null);
        Guidance guidance = new NoOpGuidance();
        JUnit5FuzzConfig.installGuidance(guidance);
        assertTrue(configFor("plain").isFuzzingMode());
        assertEquals(guidance, JUnit5FuzzConfig.installedGuidance());
    }

    @Test
    void trialsComeFromAnnotationAndSystemPropertyOverrides() throws NoSuchMethodException {
        assertEquals(Long.valueOf(1234), configFor("withTrials").maxTrials());
        // A plain method with no trial limit reports null (no bound from the annotation).
        assertNull(configFor("plain").maxTrials());
        // The system property overrides the annotation for every method.
        System.setProperty("jqf.fuzz.trials", "42");
        assertEquals(Long.valueOf(42), configFor("withTrials").maxTrials());
        assertEquals(Long.valueOf(42), configFor("plain").maxTrials());
    }

    @Test
    void durationComesFromAnnotationAndSystemPropertyOverrides() throws NoSuchMethodException {
        assertEquals(Duration.ofMinutes(5), configFor("withDuration").maxDuration());
        System.setProperty("jqf.fuzz.duration", "30s");
        assertEquals(Duration.ofSeconds(30), configFor("withDuration").maxDuration());
    }

    @Test
    void outputDirectoryIsNamedAfterClassAndMethod() throws NoSuchMethodException {
        String path = configFor("plain").outputDirectory().getPath();
        assertTrue(path.contains(Samples.class.getName()), path);
        assertTrue(path.endsWith("plain"), path);
    }

    @Test
    void regressionAlwaysIncludesAnEmptyInput() throws NoSuchMethodException {
        // With no corpus and no seeds, the only regression input is the empty one.
        assertEquals(1, configFor("plain").regressionInputs().size());
        assertEquals("zeros", configFor("plain").regressionInputs().get(0).name());
    }

    @Test
    void durationParserAcceptsCommonForms() {
        assertNull(JUnit5FuzzConfig.parseDuration(null));
        assertNull(JUnit5FuzzConfig.parseDuration(""));
        assertNull(JUnit5FuzzConfig.parseDuration("   "));
        assertEquals(Duration.ofSeconds(45), JUnit5FuzzConfig.parseDuration("45"));
        assertEquals(Duration.ofSeconds(30), JUnit5FuzzConfig.parseDuration("30s"));
        assertEquals(Duration.ofMinutes(10), JUnit5FuzzConfig.parseDuration("10m"));
        assertEquals(Duration.ofHours(2), JUnit5FuzzConfig.parseDuration("2h"));
        assertEquals(Duration.ofDays(1), JUnit5FuzzConfig.parseDuration("1d"));
    }

    @Test
    void durationParserRejectsGarbage() {
        assertThrows(IllegalArgumentException.class, () -> JUnit5FuzzConfig.parseDuration("soon"));
        assertThrows(IllegalArgumentException.class, () -> JUnit5FuzzConfig.parseDuration("10x"));
    }

    /** A do-nothing guidance, enough to stand in as an installed guidance. */
    private static final class NoOpGuidance implements Guidance {
        @Override
        public InputStream getInput() {
            return new InputStream() {
                @Override
                public int read() {
                    return 0;
                }
            };
        }

        @Override
        public boolean hasInput() {
            return false;
        }

        @Override
        public void handleResult(Result result, Throwable error) {
        }

        @Override
        public Consumer<TraceEvent> generateCallBack(Thread thread) {
            return event -> {
            };
        }
    }
}
