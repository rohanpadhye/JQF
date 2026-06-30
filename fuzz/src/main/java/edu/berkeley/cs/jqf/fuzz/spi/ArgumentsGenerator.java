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
package edu.berkeley.cs.jqf.fuzz.spi;

import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;

/**
 * Turns a guided source of randomness into the arguments for one trial.
 *
 * <p>This is the extension point that decouples the fuzzing engine from any
 * particular generation library. The engine owns the byte stream (steered by
 * the {@code Guidance}); an implementation decides how to map those bytes onto
 * structured arguments for the test method. The reference implementation wraps
 * junit-quickcheck generators, but any framework (Instancio, jetCheck, a custom
 * binding) can implement this contract without the engine knowing about it.
 *
 * <p>An instance is bound to a single test method and is reused across trials;
 * {@link #generate} is called once per trial.
 *
 * @see ArgumentsGeneratorFactory
 */
public interface ArgumentsGenerator {

    /**
     * Generates one trial's arguments from the given source of randomness.
     *
     * <p>The array length equals the test method's parameter count. Reading
     * past the end of the input typically surfaces as a {@link SkipTrialException},
     * which the engine treats as a signal to move on to the next input rather
     * than as a test failure.
     *
     * @param random the guided source of randomness for this trial
     * @return the arguments to pass to the test method
     * @throws SkipTrialException if this input cannot produce valid arguments
     *                            and the trial should be skipped
     */
    Object[] generate(StreamBackedRandom random);
}
