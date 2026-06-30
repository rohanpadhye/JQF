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
package edu.berkeley.cs.jqf.fuzz.spi;

/**
 * Runs a single trial of a test method with generated arguments.
 *
 * <p>This is the seam between the engine's fuzzing loop and a test framework.
 * An implementation creates a fresh test instance per trial and honours that
 * framework's per-method lifecycle (for example {@code @Before}/{@code @After}
 * under JUnit 4, or {@code @BeforeEach}/{@code @AfterEach} under JUnit 5). The
 * engine never references framework types; it only calls {@link #runTrial}.
 *
 * <p>An instance is bound to a single test method and reused across trials.
 *
 * @see TrialExecutorFactory
 */
public interface TrialExecutor {

    /**
     * Runs one trial with the given arguments.
     *
     * <p>Throwables raised by the test body propagate unchanged so the engine
     * can classify them (failure, unmet assumption, timeout). Implementations
     * must not swallow them.
     *
     * @param args the arguments for this trial
     * @throws Throwable whatever the test body raises
     */
    void runTrial(Object[] args) throws Throwable;

    /**
     * Returns the value returned by the most recent trial's test method.
     *
     * <p>Differential fuzzing compares these return values across runs; ordinary
     * test runners discard them. The default returns {@code null} for executors
     * that do not capture the return value.
     *
     * @return the most recent return value, or {@code null}
     */
    default Object getLastOutput() {
        return null;
    }
}
