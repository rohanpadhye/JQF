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
package edu.berkeley.cs.jqf.fuzz.junit;

import edu.berkeley.cs.jqf.fuzz.spi.FuzzFramework;
import edu.berkeley.cs.jqf.fuzz.spi.ResultClassifier;
import edu.berkeley.cs.jqf.fuzz.spi.TrialExecutorFactory;

/**
 * The JUnit 4 execution provider, discoverable through {@link java.util.ServiceLoader}.
 *
 * <p>Supplies a {@link Junit4TrialExecutorFactory} and a
 * {@link Junit4ResultClassifier} so command-line and build-tool launches can run
 * trials under JUnit 4 without compile-time coupling to this package.
 */
public final class Junit4FuzzFramework implements FuzzFramework {

    @Override
    public TrialExecutorFactory executor() {
        return new Junit4TrialExecutorFactory();
    }

    @Override
    public ResultClassifier classifier() {
        return new Junit4ResultClassifier();
    }
}
