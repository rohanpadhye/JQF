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
package edu.berkeley.cs.jqf.fuzz.instancio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import edu.berkeley.cs.jqf.junit5.FuzzTest;
import edu.berkeley.cs.jqf.junit5.JUnit5FuzzConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * End-to-end test driving a JUnit 5 {@code @FuzzTest} whose arguments come from
 * Instancio, through the standard {@code junit-jupiter-engine}.
 *
 * <p>No {@code @FuzzTest(arguments = ...)} is set, so the engine resolves the
 * provider through {@link java.util.ServiceLoader}. This module's only provider is
 * {@link InstancioArgumentsGeneratorFactory}, which is exactly the "swap the
 * generator on the classpath" path: dropping in {@code jqf-generator-instancio}
 * instead of {@code jqf-generator-quickcheck} changes how arguments are built,
 * with no change to the test.
 */
class InstancioFuzzTestEndToEndTest {

    @BeforeEach
    @AfterEach
    void resetState() {
        JUnit5FuzzConfig.clearInstalledGuidance();
        System.clearProperty("jqf.fuzz");
        System.clearProperty("jqf.fuzz.trials");
        System.clearProperty("jqf.fuzz.out");
    }

    @Test
    void regressionModeIsGreenAndBounded() throws IOException {
        assumeTrue(System.getenv("JQF_FUZZ") == null);
        // A fresh, empty output directory means no saved corpus: only the empty input runs.
        Path out = Files.createTempDirectory("jqf-instancio-regression");
        System.setProperty("jqf.fuzz.out", out.toString());

        TestExecutionSummary summary = run(InstancioScenario.class);

        assertEquals(0, summary.getTestsFailedCount(), "Instancio-generated objects should pass the assertions");
        // Exactly one bounded invocation (the empty input), not a campaign.
        assertEquals(1, summary.getTestsSucceededCount(), "regression should run exactly the one empty input");
    }

    @Test
    void fuzzingModeRunsABoundedCampaign() throws IOException {
        assumeTrue(System.getenv("JQF_FUZZ") == null);
        Path out = Files.createTempDirectory("jqf-instancio-fuzz");
        System.setProperty("jqf.fuzz", "true");
        System.setProperty("jqf.fuzz.trials", "500");
        System.setProperty("jqf.fuzz.out", out.toString());

        TestExecutionSummary summary = run(InstancioScenario.class);

        // The target has no planted bug, so a full campaign of Instancio-built objects stays green.
        assertEquals(0, summary.getTestsFailedCount(), "a clean target should not fail under fuzzing");
        assertEquals(1, summary.getTestsSucceededCount(), "the single @FuzzTest node should pass");
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
     * A clean fuzz target whose single parameter is an object built by Instancio.
     * Instancio populates every field, so the assertions hold for every trial.
     */
    static class InstancioScenario {
        @FuzzTest
        void acceptsPopulatedObject(Order order) {
            assertNotNull(order, "Instancio should never hand the test a null object");
            assertNotNull(order.getCustomer(), "Instancio should populate the nested object");
            assertNotNull(order.getCustomer().getName(), "Instancio should populate nested String fields");
        }
    }

    public static class Order {
        private String id;
        private int quantity;
        private Customer customer;

        public String getId() {
            return id;
        }

        public int getQuantity() {
            return quantity;
        }

        public Customer getCustomer() {
            return customer;
        }
    }

    public static class Customer {
        private String name;
        private String email;

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }
    }
}
