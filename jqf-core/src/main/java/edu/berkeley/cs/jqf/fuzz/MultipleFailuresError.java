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
package edu.berkeley.cs.jqf.fuzz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregates several trial failures from one fuzzing campaign into a single error.
 *
 * <p>The engine throws this when more than one input fails, in place of JUnit 4's
 * {@code MultipleFailureException}, so the fuzzing loop stays free of any
 * test-framework dependency. A framework adapter may translate it back into its
 * own aggregate type for richer reporting.
 */
public class MultipleFailuresError extends Error {

    private static final long serialVersionUID = 1L;

    private final List<Throwable> failures;

    /**
     * Creates an aggregate error from the given failures.
     *
     * @param failures the failing throwables to bundle
     */
    public MultipleFailuresError(List<Throwable> failures) {
        this.failures = Collections.unmodifiableList(
                new ArrayList<>(failures == null ? Collections.emptyList() : failures));
    }

    /**
     * Returns the individual failures bundled in this error.
     *
     * @return an unmodifiable list of the failing throwables
     */
    public List<Throwable> getFailures() {
        return failures;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder()
                .append("There were ").append(failures.size()).append(" failures:");
        for (Throwable failure : failures) {
            sb.append("\n\t").append(failure);
        }
        return sb.toString();
    }
}
