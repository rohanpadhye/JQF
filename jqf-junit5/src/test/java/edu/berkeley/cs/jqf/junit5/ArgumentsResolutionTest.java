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

import java.lang.reflect.Method;
import java.util.Collections;

import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGenerator;
import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGeneratorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for argument-generator resolution, including the fail-fast behaviour
 * when no provider is on the classpath. This module deliberately has no
 * {@link ArgumentsGeneratorFactory} provider at test scope, so resolution through
 * {@link java.util.ServiceLoader} finds nothing here.
 */
class ArgumentsResolutionTest {

    /** A trivial generator factory used as an explicit override. */
    public static final class FixedFactory implements ArgumentsGeneratorFactory {
        @Override
        public ArgumentsGenerator create(Class<?> testClass, Method testMethod) {
            return random -> new Object[0];
        }
    }

    @Test
    void failsFastWithAClearMessageWhenNoProviderIsAvailable() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> JUnit5FuzzRunner.resolveFactory(ArgumentsGeneratorFactory.class, Collections.emptyList()));
        assertTrue(e.getMessage().contains("No argument-generator provider"), e.getMessage());
        assertTrue(e.getMessage().contains("jqf-generator-quickcheck"), e.getMessage());
    }

    @Test
    void failsFastWhenTheMarkerIsNullAndNoProviderIsAvailable() {
        assertThrows(IllegalStateException.class,
                () -> JUnit5FuzzRunner.resolveFactory(null, Collections.emptyList()));
    }

    @Test
    void usesTheFirstServiceLoaderProviderWhenNoOverrideIsGiven() {
        FixedFactory provided = new FixedFactory();
        ArgumentsGeneratorFactory resolved =
                JUnit5FuzzRunner.resolveFactory(ArgumentsGeneratorFactory.class, Collections.singletonList(provided));
        assertSame(provided, resolved);
    }

    @Test
    void honoursAnExplicitOverrideEvenWithNoServiceLoaderProvider() {
        ArgumentsGeneratorFactory resolved =
                JUnit5FuzzRunner.resolveFactory(FixedFactory.class, Collections.emptyList());
        assertTrue(resolved instanceof FixedFactory);
    }

    @Test
    void resolveArgumentsFailsFastForAFuzzTestWithoutAProvider() throws NoSuchMethodException {
        // No provider on the classpath, so the default-marker path throws.
        Method method = Holder.class.getDeclaredMethod("target", int.class);
        assertThrows(IllegalStateException.class,
                () -> JUnit5FuzzRunner.resolveArguments(Holder.class, method));
    }

    @Test
    void decodeDrivesTheGeneratorWithTheInputStream() {
        ArgumentsGenerator generator = input -> {
            StreamBackedRandom random = new StreamBackedRandom(input);
            return new Object[] {random.nextInt(), "ok"};
        };
        Object[] args = JUnit5FuzzRunner.decode(generator, new java.io.ByteArrayInputStream(new byte[16]));
        assertTrue(args.length == 2);
    }

    static class Holder {
        @FuzzTest
        void target(int x) {
        }
    }
}
