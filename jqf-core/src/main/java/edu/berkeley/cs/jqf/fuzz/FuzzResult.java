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
 * The outcome of a fuzzing campaign for one test method.
 *
 * <p>This is the engine-level result returned to command-line drivers and the
 * build-tool plugin. It replaces {@code org.junit.runner.Result} on those paths
 * so the engine carries no test-framework dependency.
 */
public final class FuzzResult {

    private final List<Throwable> failures;

    /**
     * Creates a result from the failures collected during the campaign.
     *
     * @param failures the throwables that failed trials; an empty list means success
     */
    public FuzzResult(List<Throwable> failures) {
        this.failures = Collections.unmodifiableList(
                new ArrayList<>(failures == null ? Collections.emptyList() : failures));
    }

    /**
     * Reports whether the campaign found no failing inputs.
     *
     * @return {@code true} if no failures were recorded
     */
    public boolean wasSuccessful() {
        return failures.isEmpty();
    }

    /**
     * Returns the failures collected during the campaign.
     *
     * @return an unmodifiable list of failures, empty on success
     */
    public List<Throwable> getFailures() {
        return failures;
    }
}
