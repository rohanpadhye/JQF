/*
 * Copyright (c) 2017, University of California, Berkeley
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
package edu.berkeley.cs.jqf.fuzz.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Maps integer keys to integer counts using a fixed-size table.
 *
 * Hash collisions are completely ignored; therefore, the counts
 * are unreliable.
 *
 * @author Rohan Padhye
 */
public class Counter {

    /** The size of the counter map. */
    private final int size;

    /** The counter map as an array of integers. */
    private final int[] counts;

    public Counter(int size) {
        this.size = size;
        this.counts = new int[size];
    }

    public void clear() {
        for (int i = 0; i < counts.length; i++) {
            this.counts[i] = 0;
        }
    }

    private int idx(int key) {
        return Hashing.hash(key, size);
    }

    public int increment(int key) {
        return ++this.counts[idx(key)];
    }

    public int increment(int key, int delta) {
        return (this.counts[idx(key)] += delta);
    }

    public Collection<Integer> nonZeroValues() {
        List<Integer> values = new ArrayList<>(size /2);
        for (int count : counts) {
            if (count != 0) {
                values.add(count);
            }
        }
        return values;
    }

    public int[] getCounts() {
        return this.counts;
    }

    public int get(int key) {
        return this.counts[idx(key)];
    }
}
