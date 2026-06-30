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

import edu.berkeley.cs.jqf.fuzz.guidance.TimeoutException;

/**
 * Classifies a throwable raised by a trial into an engine-level outcome.
 *
 * <p>The engine must distinguish an unmet assumption (discard this input as
 * invalid) from a genuine failure, but it cannot depend on a test framework's
 * assumption type. Each adapter supplies its own predicate instead: JUnit 4
 * recognises {@code org.junit.AssumptionViolatedException}, JUnit 5 recognises
 * {@code org.opentest4j.TestAbortedException}. This keeps the engine free of any
 * test-framework dependency.
 */
public interface ResultClassifier {

    /**
     * Reports whether the throwable signals an unmet assumption.
     *
     * <p>An unmet assumption means the generated input did not satisfy the
     * test's preconditions, so the engine discards it as invalid rather than
     * recording a failure.
     *
     * @param t a throwable raised by a trial
     * @return {@code true} if the input should be treated as invalid
     */
    boolean isAssumptionViolation(Throwable t);

    /**
     * Reports whether the throwable is a fuzzing timeout.
     *
     * <p>The default recognises the engine's own {@link TimeoutException}, which
     * is framework-independent.
     *
     * @param t a throwable raised by a trial
     * @return {@code true} if the trial timed out
     */
    default boolean isTimeout(Throwable t) {
        return t instanceof TimeoutException;
    }
}
