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
public class MapOfCounters {

    private static final int TABLE_SIZE = 6151; // Between 2^12 and 2^13

    private int[] counts = new int[TABLE_SIZE*TABLE_SIZE];

    public void clear() {
        for (int i = 0; i < counts.length; i++) {
            this.counts[i] = 0;
        }
    }

    private int idx(int k1, int k2) {
        return Hashing.hash(k1, TABLE_SIZE) * TABLE_SIZE + Hashing.hash(k2, TABLE_SIZE);
    }

    public void increment(int k1, int k2) {
        this.counts[idx(k1, k2)]++;
    }

    public Collection<Integer> nonZeroValues(int k1) {
        List<Integer> values = new ArrayList<>(TABLE_SIZE);
        int lower = k1 + TABLE_SIZE;
        int upper = lower + TABLE_SIZE;
        for (int i = lower; i < upper; i++) {
            if (counts[i] > 0) {
                values.add(counts[i]);
            }
        }
        return values;
    }

    public Collection<Integer> keys() {
        List<Integer> keys = new ArrayList<>(TABLE_SIZE);
        for (int i = 0; i < TABLE_SIZE; i++) {
            keys.add(i);
        }
        return keys;
    }


}
