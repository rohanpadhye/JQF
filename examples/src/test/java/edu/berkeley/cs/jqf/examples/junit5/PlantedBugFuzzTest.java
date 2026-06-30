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
package edu.berkeley.cs.jqf.examples.junit5;

import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.junit5.FuzzTest;

/**
 * A JUnit 5 {@code @FuzzTest} with a planted bug, used to show that the Maven
 * plugin can fuzz and replay {@code @FuzzTest} methods.
 *
 * <p>The method fails for any negative input. The empty (all-zero) regression
 * input decodes to {@code 0}, which passes, so only a fuzzing campaign reaches
 * the bug. The {@code int} comes from a junit-quickcheck generator, supplied by
 * the {@code jqf-generator-quickcheck} provider on the test classpath.
 */
public class PlantedBugFuzzTest {

    public static class IntGenerator extends Generator<Integer> {
        public IntGenerator() {
            super(Integer.class);
        }

        @Override
        public Integer generate(SourceOfRandomness random, GenerationStatus status) {
            return random.nextInt();
        }
    }

    @FuzzTest
    public void mustBeNonNegative(@From(IntGenerator.class) Integer value) {
        if (value < 0) {
            throw new AssertionError("planted bug: negative input " + value);
        }
    }
}
