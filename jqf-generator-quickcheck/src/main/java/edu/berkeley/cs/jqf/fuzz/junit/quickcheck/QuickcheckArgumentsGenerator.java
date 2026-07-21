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

import java.io.InputStream;
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
 * the guided byte stream is wrapped in a {@link StreamBackedRandom} and adapted
 * to a junit-quickcheck {@link SourceOfRandomness} so the generators consume it.
 * Running out of input surfaces as an {@code IllegalStateException} wrapping an
 * {@code EOFException}, which the engine treats as a skipped trial.
 *
 * <p>The {@link StreamBackedRandom} is built with an eight-byte leading-ignore
 * window because {@link SourceOfRandomness}'s constructor reads eight bytes to
 * seed itself and then discards them; ignoring them keeps generation reading
 * from the first real input byte. This is a junit-quickcheck quirk, so it lives
 * here rather than in the engine.
 */
public final class QuickcheckArgumentsGenerator implements ArgumentsGenerator {

    private final List<Generator<?>> generators;

    QuickcheckArgumentsGenerator(List<Generator<?>> generators) {
        this.generators = generators;
    }

    @Override
    public Object[] generate(InputStream input) {
        StreamBackedRandom random = new StreamBackedRandom(input, Long.BYTES);
        SourceOfRandomness sourceOfRandomness = new FastSourceOfRandomness(random);
        GenerationStatus genStatus = new NonTrackingGenerationStatus(sourceOfRandomness);
        return generators.stream()
                .map(g -> g.generate(sourceOfRandomness, genStatus))
                .toArray();
    }
}
