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

import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An extension of {@link Counter} that caches which entries
 * are non-zero.
 *
 * <p>This version of counter is used by classes such as
 * {@link Coverage}, which require frequent queries of
 * the non-zero locations.</p>
 */
public class NonZeroCachingCounter extends Counter {

    private int nonZeroCount;

    private IntArrayList nonZeroIndices;

    public NonZeroCachingCounter(int size) {
        super(size);
        this.nonZeroCount = 0;
        this.nonZeroIndices = new IntArrayList();
    }

    @Override
    public void clear() {
        IntIterator iter = nonZeroIndices.intIterator();
        while(iter.hasNext()){
            int idx = iter.next();
            counts[idx] = 0;
        }
        this.nonZeroCount = 0;
        this.nonZeroIndices = new IntArrayList();
    }


    @Override
    public int incrementAtIndex(int index, int delta) {
        int newValue = super.incrementAtIndex(index, delta);
        // A count becomes non-zero if it was incremented to delta
        if (newValue == delta) {
            nonZeroIndices.add(index);
            nonZeroCount++;
        }
        return newValue;
    }

    @Override
    public int getNonZeroSize() {
        return nonZeroCount;
    }

    @Override
    public boolean hasNonZeros(){
        return nonZeroCount > 0;
    }

    @Override
    public IntList getNonZeroIndices() {
        return nonZeroIndices;
    }

    @Override
    public IntList getNonZeroValues() {
        IntArrayList values = new IntArrayList(this.size / 2);
        IntIterator iter = nonZeroIndices.intIterator();
        while(iter.hasNext()){
            int idx = iter.next();
            int count = counts[idx];
            assert (count != 0);
            values.add(count);
        }
        return values;
    }

    @Override
    public void setAtIndex(int index, int newValue) {
        int oldValue = counts[index];
        super.setAtIndex(index, newValue);
        if (oldValue == 0 && newValue != 0) {
            nonZeroIndices.add(index);
            nonZeroCount++;
        }
    }


}
