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
package edu.berkeley.cs.jqf.fuzz.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

    /** The number of counters to map. */
    private final int numCounters;

    /** The size of each counter in the map. */
    private final int counterSize;

    /** The table of counters. */
    private final Counter[] counters;

    public MapOfCounters(int numCounters, int counterSize) {
        this.numCounters = numCounters;
        this.counterSize = counterSize;
        this.counters = new Counter[numCounters];
    }

    public void clear() {
        for (int i = 0; i < counters.length; i++) {
            counters[i] = null;
        }
    }

    private int idx(int key) {
        return Hashing.hash(key, numCounters);
    }

    public void increment(int k1, int k2) {
        int idx = idx(k1);
        if (counters[idx] == null) {
            counters[idx] = new Counter(counterSize);
        }
        counters[idx].increment(k2);
    }

    public Collection<Integer> nonZeroCountsAtIndex(int idx) {
        if (counters[idx] != null) {
            return counters[idx].getNonZeroValues();
        } else {
            return Collections.emptyList();
        }

    }

    public Collection<Integer> nonEmptyCountersIndices() {
        List<Integer> keys = new ArrayList<>(numCounters);
        for (int i = 0; i < numCounters; i++) {
            if (counters[i] != null) {
                keys.add(i);
            }
        }
        return keys;
    }

}
