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
import edu.berkeley.cs.jqf.examples.simple.SimpleClass;
import edu.berkeley.cs.jqf.junit5.FuzzTest;

/**
 * The JUnit 5 counterpart of
 * {@link edu.berkeley.cs.jqf.examples.simple.SimpleClassTest}.
 *
 * <p>The JUnit 4 version uses {@code @RunWith(JQF.class)} on the class and
 * {@code @Fuzz} on the method. The migration to JUnit 5 is small: drop the
 * class-level runner and mark the method {@link FuzzTest @FuzzTest}. The argument
 * generators are unchanged ({@code @From} still resolves a junit-quickcheck
 * {@link Generator}), supplied by the {@code jqf-generator-quickcheck} provider on
 * the test classpath.
 *
 * <p>How it runs:
 * <ul>
 *   <li>A plain {@code mvn test} replays the saved corpus and seeds (bounded and
 *       fast) under the normal Jupiter lifecycle.</li>
 *   <li>{@code mvn test -Djqf.fuzz=true} runs a coverage-guided Zest campaign and
 *       fails the test on the first failing input.</li>
 * </ul>
 */
public class SimpleFuzzTest {

    public static class SimpleGenerator extends Generator<Integer> {
        public SimpleGenerator() {
            super(Integer.class);
        }

        @Override
        public Integer generate(SourceOfRandomness sourceOfRandomness, GenerationStatus generationStatus) {
            return sourceOfRandomness.nextInt();
        }
    }

    @FuzzTest
    public void testWithGenerator(@From(SimpleGenerator.class) Integer a) {
        SimpleClass.test(a);
    }
}
