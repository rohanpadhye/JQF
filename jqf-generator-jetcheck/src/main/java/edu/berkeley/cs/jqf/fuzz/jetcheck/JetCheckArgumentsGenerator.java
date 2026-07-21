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
package edu.berkeley.cs.jqf.fuzz.jetcheck;

import java.io.InputStream;

import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGenerator;
import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGeneratorFactory;
import org.jetbrains.jetCheck.Generator;

/**
 * Generates one trial's arguments by running the per-parameter jetCheck generators
 * against the engine's guided byte stream.
 *
 * <p>Each value is produced by {@link JetCheckGeneration}, so the bytes the engine
 * supplies determine the arguments and replaying the same bytes rebuilds them. If a
 * generator cannot produce a value for the current input, the whole trial is skipped.
 *
 * <p>A custom {@link ArgumentsGeneratorFactory} for its own parameter types can reuse
 * this loop -- the per-parameter draw and the null/skip guard -- by building one
 * through {@link #builder(Generator[])} with its own generators.
 */
public final class JetCheckArgumentsGenerator implements ArgumentsGenerator {

    /**
     * The default size hint. It biases jetCheck's list and string lengths; the exact
     * length is still drawn from the byte stream, this only sets the centre of that draw.
     * jetCheck's own default cycles 1..100, so a fixed 16 keeps generated collections small.
     */
    public static final int DEFAULT_SIZE_HINT = 16;

    private final Generator<?>[] generators;
    private final int sizeHint;

    private JetCheckArgumentsGenerator(Generator<?>[] generators, int sizeHint) {
        this.generators = generators;
        this.sizeHint = sizeHint;
    }

    /**
     * Starts a builder for a generator over the given per-parameter jetCheck generators.
     *
     * @param generators one generator per test-method parameter, in declaration order
     * @return a builder, defaulting to {@link #DEFAULT_SIZE_HINT}
     */
    @SafeVarargs
    public static Builder builder(Generator<?>... generators) {
        return new Builder(generators);
    }

    @Override
    public Object[] generate(InputStream input) {
        StreamBackedRandom random = new StreamBackedRandom(input);
        Object[] arguments = new Object[generators.length];
        for (int i = 0; i < generators.length; i++) {
            // Propagates SkipTrialException when a generator cannot satisfy its constraint.
            arguments[i] = JetCheckGeneration.generate(generators[i], random, sizeHint);
        }
        return arguments;
    }

    /**
     * Builds a {@link JetCheckArgumentsGenerator}.
     */
    public static final class Builder {

        private final Generator<?>[] generators;
        private int sizeHint = DEFAULT_SIZE_HINT;

        private Builder(Generator<?>[] generators) {
            this.generators = generators;
        }

        /**
         * Sets the size hint that biases list and string lengths.
         *
         * @param sizeHint the size hint (default {@link JetCheckArgumentsGenerator#DEFAULT_SIZE_HINT})
         * @return this builder
         */
        public Builder sizeHint(int sizeHint) {
            this.sizeHint = sizeHint;
            return this;
        }

        /**
         * Builds the arguments generator.
         *
         * @return the arguments generator
         */
        public JetCheckArgumentsGenerator build() {
            return new JetCheckArgumentsGenerator(generators, sizeHint);
        }
    }
}
