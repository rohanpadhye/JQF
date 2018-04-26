/*
 * Copyright (c) 2018, The Regents of the University of California
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
package edu.berkeley.cs.jqf.examples.guava;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.generator.Size;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * @author Rohan Padhye
 */
public class CacheRequestsGenerator extends Generator<int[]> {

    private Size size;
    private InRange range;

    public CacheRequestsGenerator() {
        super(int[].class);
    }

    public void configure(Size size) {
        this.size = size;
    }

    public void configure(InRange range) {
        this.range = range;
    }

    @Override
    public int[] generate(SourceOfRandomness r, GenerationStatus generationStatus) {
        int minSize = size != null ? size.min() : 0;
        int maxSize = size != null ? size.max() : Integer.MAX_VALUE;
        int size = r.nextInt(minSize, maxSize);
        int[] requests = new int[size];

        int minRange = range != null ? range.minInt() : 0;
        int maxRange = range != null ? range.maxInt() : 1024;

        for (int i = 0; i < size; i++) {
            int request;
            // Maybe re-use
            if (i > 0 && r.nextBoolean()) {
                int idx = r.nextInt(0, i);
                request = requests[idx];
            } else {
                request = r.nextInt(minRange, maxRange);
            }
            requests[i] = request;
        }

        return requests;
    }
}
