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
import java.util.List;

/**
 * Maps integer keys to integer counts using a fixed-size table.
 *
 * <p>Hash collisions are completely ignored; therefore, the counts
 * are unreliable.</p>
 *
 * <p>Throughout the internal documentation, the term "key" is used
 * to refer to the keys that are hashed, while "index" is used to
 * the result of key-hashing, i.e. the location in the internal
 * array storage.</p>
 *
 * @author Rohan Padhye
 */
public class Counter {

    /** The size of the counter map. */
    protected final int size;

    /** The counter map as an array of integers. */
    protected final int[] counts;

    /**
     * Creates a new counter with given size.
     *
     * @param size the fixed-number of elements in the hashtable.
     */
    public Counter(int size) {
        this.size = size;
        this.counts = new int[size];
    }

    /**
     * Returns the size of this counter.
     *
     * @return the size of this counter
     */
    public int size() {
        return this.size;
    }

    /**
     * Clears the counter by setting all values to zero.
     */
    public void clear() {
        for (int i = 0; i < counts.length; i++) {
            this.counts[i] = 0;
        }
    }

    private int idx(int key) {
        return Hashing.hash(key, size);
    }

    protected int incrementAtIndex(int index, int delta) {
        return (this.counts[index] += delta);
    }

    /**
     * Increments the count at the given key.
     *
     * <p>Note that the key is hashed and therefore the count
     * to increment may be shared with another key that hashes
     * to the same value. </p>
     *
     * @param key the key whose count to increment
     * @return the new value after incrementing the count
     */
    public int increment(int key) {
        return incrementAtIndex(idx(key), 1);
    }

    /**
     *
     * Increments the count at the given key by a given delta.
     *
     * <p>Note that the key is hashed and therefore the count
     * to increment may be shared with another key that hashes
     * to the same value. </p>
     *
     * @param key the key whose count to increment
     * @param delta the amount to increment by
     * @return the new value after incrementing the count
     */
    public int increment(int key, int delta) {
        return incrementAtIndex(idx(key), delta);
    }

    /**
     * Returns the number of indices with non-zero counts.
     *
     * @return the number of indices with non-zero counts
     */
    public int getNonZeroSize() {
        int size = 0;
        for (int i = 0; i < counts.length; i++) {
            int count = counts[i];
            if (count != 0) {
                size++;
            }
        }
        return size;
    }


    /**
     * Returns a set of indices at which the count is non-zero.
     *
     * <p>Note that indices are different from keys, in that
     * multiple keys can map to the same index due to hash
     * collisions.</p>
     *
     * @return a set of indices at which the count is non-zero
     */
    public Collection<Integer> getNonZeroIndices() {
        List<Integer> indices = new ArrayList<>(size /2);
        for (int i = 0; i < counts.length; i++) {
            int count = counts[i];
            if (count != 0) {
                indices.add(i);
            }
        }
        return indices;
    }

    /**
     * Returns a set of non-zero count values in this counter.
     *
     * @return a set of non-zero count values in this counter.
     */
    public Collection<Integer> getNonZeroValues() {
        List<Integer> values = new ArrayList<>(size /2);
        for (int i = 0; i < counts.length; i++) {
            int count = counts[i];
            if (count != 0) {
                values.add(count);
            }
        }
        return values;
    }

    /**
     * Retreives a value for a given key.
     *
     * <p>The key is first hashed to retreive a value from
     * the counter, and hence the result is modulo collisions.</p>
     *
     * @param key the key to query
     * @return the count for the index corresponding to this key
     */
    public int get(int key) {
        return this.counts[idx(key)];
    }


    public int getAtIndex(int idx) {
        return this.counts[idx];
    }

    public void setAtIndex(int idx, int value) {
        this.counts[idx] = value;
    }
}
