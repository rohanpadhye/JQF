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

import edu.berkeley.cs.jqf.fuzz.guidance.TimeoutException;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link Junit5ResultClassifier}: a JUnit 5 unmet assumption is an
 * {@link TestAbortedException}, and the engine timeout is recognised by default.
 */
class Junit5ResultClassifierTest {

    private final Junit5ResultClassifier classifier = new Junit5ResultClassifier();

    @Test
    void testAbortedExceptionIsAnAssumptionViolation() {
        assertTrue(classifier.isAssumptionViolation(new TestAbortedException("assume failed")));
    }

    @Test
    void otherThrowablesAreNotAssumptionViolations() {
        assertFalse(classifier.isAssumptionViolation(new RuntimeException("boom")));
        assertFalse(classifier.isAssumptionViolation(new AssertionError("nope")));
    }

    @Test
    void engineTimeoutIsRecognised() {
        assertTrue(classifier.isTimeout(new TimeoutException()));
        assertFalse(classifier.isTimeout(new RuntimeException("not a timeout")));
    }
}
