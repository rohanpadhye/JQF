/*
 * Copyright (c) 2017-2018 The Regents of the University of California
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.internal.GeometricDistribution;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * Provides a generation status that does not track the number of trials
 * generated so far. This is useful for guided fuzzing where the burden
 * of making choices is on the guidance system rather than on quickcheck.
 *
 * @author Rohan Padhye
 */
public class NonTrackingGenerationStatus implements GenerationStatus {

    public static final int MEAN_SIZE = 10;

    private final SourceOfRandomness random;
    private final Map<Key<?>, Object> contextValues = new HashMap<>();
    private final GeometricDistribution geometric = new GeometricDistribution();


    public NonTrackingGenerationStatus(SourceOfRandomness random) {
        this.random = random;
    }

    @Override
    public int size() {
        return geometric.sampleWithMean(MEAN_SIZE, random);
    }

    @Override
    public int attempts() {
        throw new UnsupportedOperationException("attempts() and @ValueOf" +
                " is not supported in guided mode.");
    }

    @Override
    public <T> GenerationStatus setValue(Key<T> key, T value) {
        contextValues.put(key, value);
        return this;
    }

    @Override
    public <T> Optional<T> valueOf(Key<T> key) {
        return Optional.ofNullable(key.cast(contextValues.get(key)));
    }
}
