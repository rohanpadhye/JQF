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

import java.util.Random;

import edu.berkeley.cs.jqf.fuzz.spi.SkipTrialException;
import org.jetbrains.jetCheck.CannotSatisfyCondition;
import org.jetbrains.jetCheck.GenerationEnvironment;
import org.jetbrains.jetCheck.Generator;
import org.jetbrains.jetCheck.IntSource;

/**
 * Runs a jetCheck {@link Generator} against JQF's guided byte stream.
 *
 * <p>Since jetCheck 0.3.0, {@link GenerationEnvironment#generative(IntSource, int)} is
 * public, so this drives generation through public API only. jetCheck reads ints
 * through an {@link IntSource}; routing each draw to a {@link Random} that reads the
 * engine's stream keeps generation byte-structured, because
 * {@code IntDistribution.generateInt(Random)} consumes only the bytes each draw needs.
 */
public final class JetCheckGeneration {

    private JetCheckGeneration() {
    }

    /**
     * Generates one non-null value from {@code generator}, drawing every int from {@code random}.
     *
     * <p>A constrained generator ({@code nonEmptyLists}, {@code suchThat}) can fail to satisfy its
     * constraint from the current input -- most often the all-zero "zeros" seed. jetCheck then
     * throws {@link CannotSatisfyCondition}, or a mapped generator yields {@code null}. Either way
     * this reports a {@link SkipTrialException}, the engine's standard skip signal, so the input is
     * dropped rather than reaching the test as a leaked jetCheck exception or a spurious
     * {@link NullPointerException}.
     *
     * @param generator the jetCheck generator bound to a parameter type
     * @param random    the guided source of randomness (JQF's {@code StreamBackedRandom})
     * @param sizeHint  the collection-size hint jetCheck uses to bias list and string lengths
     * @param <T>       the generated type
     * @return a generated value, never {@code null}
     * @throws SkipTrialException if the generator cannot produce a value for this input
     */
    public static <T> T generate(Generator<T> generator, Random random, int sizeHint) {
        IntSource source = distribution -> distribution.generateInt(random);
        T value;
        try {
            value = GenerationEnvironment.generative(source, sizeHint).generate(generator);
        } catch (CannotSatisfyCondition e) {
            // A constrained generator that cannot satisfy its condition from this input; report the
            // engine's skip signal rather than leaking a jetCheck exception to the caller.
            throw new SkipTrialException(e);
        }
        if (value == null) {
            // A mapped generator can also yield null outright; skip rather than hand null to a test.
            throw new SkipTrialException();
        }
        return value;
    }
}
