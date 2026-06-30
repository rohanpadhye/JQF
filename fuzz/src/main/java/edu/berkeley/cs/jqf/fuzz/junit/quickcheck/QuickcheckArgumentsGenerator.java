/*
 * Copyright (c) 2017-2018 The Regents of the University of California
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
package edu.berkeley.cs.jqf.fuzz.junit.quickcheck;

import java.util.List;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGenerator;

/**
 * An {@link ArgumentsGenerator} backed by junit-quickcheck generators.
 *
 * <p>Each parameter has its own pre-resolved {@link Generator}. On every trial,
 * the file-backed source of randomness is adapted to a junit-quickcheck
 * {@link SourceOfRandomness} so the generators consume the guided byte stream.
 * Running out of input surfaces as an {@code IllegalStateException} wrapping an
 * {@code EOFException}, which the engine treats as a skipped trial.
 */
public final class QuickcheckArgumentsGenerator implements ArgumentsGenerator {

    private final List<Generator<?>> generators;

    QuickcheckArgumentsGenerator(List<Generator<?>> generators) {
        this.generators = generators;
    }

    @Override
    public Object[] generate(StreamBackedRandom random) {
        SourceOfRandomness sourceOfRandomness = new FastSourceOfRandomness(random);
        GenerationStatus genStatus = new NonTrackingGenerationStatus(sourceOfRandomness);
        return generators.stream()
                .map(g -> g.generate(sourceOfRandomness, genStatus))
                .toArray();
    }
}
