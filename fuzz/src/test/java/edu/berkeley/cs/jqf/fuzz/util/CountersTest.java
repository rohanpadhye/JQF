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

import java.util.Collection;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import edu.berkeley.cs.jqf.fuzz.util.Counter;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.runner.RunWith;

@RunWith(JUnitQuickcheck.class)
public class CountersTest {

    private static final int COUNTER_SIZE = 6151;

    @Property
    public void incrementWorks(int[] indexes) {
        Counter counter = new Counter(COUNTER_SIZE);
        // Check local state
        for (int index : indexes) {
            int before = counter.get(index);
            int after = counter.increment(index);
            Assert.assertEquals(before+1, after);
        }
        // Check global state
        int sum = 0;
        for (int i = 0; i < COUNTER_SIZE; i++) {
            sum += counter.get(i);
        }
        Assert.assertEquals(indexes.length, sum);
    }

    @Property
    public void incrementDeltaWorks(int[] indexes, int delta) {
        Counter counter = new Counter(COUNTER_SIZE);
        // Check local state
        for (int index : indexes) {
            int before = counter.get(index);
            int after = counter.increment(index, delta);
            Assert.assertEquals(before+delta, after);
        }
        // Check global state
        int sum = 0;
        for (int i = 0; i < COUNTER_SIZE; i++) {
            sum += counter.get(i);
        }
        Assert.assertEquals(indexes.length*delta, sum);
    }

    @Property
    public void incrementDeltaWorks(int index, int delta, @InRange(minInt=2, maxInt=42) int times) {
        Counter counter = new Counter(COUNTER_SIZE);
        // Check local state
        for (int j = 0; j < times; j++) {
            int before = counter.get(index);
            int after = counter.increment(index, delta);
            Assert.assertEquals(before+delta, after);
        }
        // Check global state
        int sum = 0;
        for (int i = 0; i < COUNTER_SIZE; i++) {
            sum += counter.get(i);
        }
        Assert.assertEquals(delta*times, sum);
    }

    @Property
    public void nonZeroCountsIsAccurate(int[] indexes, int delta) {
        Counter counter = new Counter(COUNTER_SIZE);
        for (int index : indexes) {
            counter.increment(index, delta);
        }

        Collection<Integer> nonZeroValues = counter.nonZeroValues();
        Assert.assertThat(nonZeroValues.size(), Matchers.lessThanOrEqualTo(indexes.length));


        int sum = 0;
        for (int v : nonZeroValues) {
            sum += v;
        }

        Assert.assertEquals(indexes.length * delta, sum);

    }

    @Property
    public void clearsToZero(int[] indexes) {
        // Fill counter with some data
        Counter counter = new Counter(COUNTER_SIZE);
        for (int index : indexes) {
            int before = counter.get(index);
            int after = counter.increment(index);
            Assert.assertEquals(before + 1, after);
        }

        // Clear it
        counter.clear();

        // Check that all are zero
        for (int i = 0; i < COUNTER_SIZE; i++) {
            Assert.assertEquals(0, counter.get(i));
        }

        for (int index : indexes) {
            Assert.assertEquals(0, counter.get(index));
        }

        Assert.assertThat(counter.nonZeroValues(), Matchers.iterableWithSize(0));
    }
}
