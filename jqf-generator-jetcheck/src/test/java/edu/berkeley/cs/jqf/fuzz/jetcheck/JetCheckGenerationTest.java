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
import java.util.List;

import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import edu.berkeley.cs.jqf.fuzz.spi.SkipTrialException;
import org.jetbrains.jetCheck.Generator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link JetCheckGeneration#generate}: it produces a value from the guided stream, and
 * skips the trial rather than returning null when jetCheck cannot generate one.
 */
class JetCheckGenerationTest {

    private static StreamBackedRandom zeros() {
        // An unbounded stream of zero bytes, matching the fuzzer's canonical "zeros" seed. jetCheck
        // reads as far as it likes, so a constrained generator retries against zeros until it gives
        // up and yields null, rather than hitting end-of-input.
        return new StreamBackedRandom(new InputStream() {
            @Override
            public int read() {
                return 0;
            }
        }, Long.BYTES);
    }

    @Test
    void skipsTheTrialWhenTheGeneratorYieldsNull() {
        // constant(null) always yields null; the helper must not hand that to the test.
        assertThrows(SkipTrialException.class,
                () -> JetCheckGeneration.generate(Generator.constant(null), zeros(), 16));
    }

    @Test
    void skipsTheTrialWhenNonEmptyListsCannotSatisfyItsConstraint() {
        // The all-zero seed cannot fill a non-empty list, so jetCheck yields null -- the case that
        // surfaced as a NullPointerException in a downstream @FuzzTest before this guard.
        Generator<List<Integer>> nonEmpty = Generator.nonEmptyLists(Generator.integers());
        assertThrows(SkipTrialException.class,
                () -> JetCheckGeneration.generate(nonEmpty, zeros(), 16));
    }

    @Test
    void returnsTheValueWhenTheGeneratorProducesOne() {
        // A total generator still returns its value; the null guard must not fire spuriously.
        Integer value = assertDoesNotThrow(
                () -> JetCheckGeneration.generate(Generator.integers(), zeros(), 16));
        assertNotNull(value);
    }
}
