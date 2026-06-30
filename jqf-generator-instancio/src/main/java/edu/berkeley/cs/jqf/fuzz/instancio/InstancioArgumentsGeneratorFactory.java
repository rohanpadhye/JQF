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

import java.lang.reflect.Method;

import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGenerator;
import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGeneratorFactory;

/**
 * Builds <a href="https://www.instancio.org/">Instancio</a>-backed
 * {@link ArgumentsGenerator}s.
 *
 * <p>This is a second reference provider, alongside the junit-quickcheck one. It
 * exists to show that {@link ArgumentsGeneratorFactory} is generation-library
 * agnostic: the engine and both run paths drive it without depending on Instancio.
 * It is discovered through {@link java.util.ServiceLoader}, or selected per test
 * with {@code @FuzzTest(arguments = InstancioArgumentsGeneratorFactory.class)}.
 *
 * <p>Instancio is a seed-based generator, so Zest steers it through a single seed
 * per parameter rather than a byte-by-byte decoding. That gives coarser control
 * than junit-quickcheck; see the module README for the trade-off.
 */
public final class InstancioArgumentsGeneratorFactory implements ArgumentsGeneratorFactory {

    @Override
    public ArgumentsGenerator create(Class<?> testClass, Method testMethod) {
        return new InstancioArgumentsGenerator(testMethod.getParameterTypes());
    }
}
