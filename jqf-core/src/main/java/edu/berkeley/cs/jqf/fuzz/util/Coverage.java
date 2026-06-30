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
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import edu.berkeley.cs.jqf.instrument.tracing.events.BranchEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEventVisitor;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

/**
 * Utility class to collect branch and function coverage
 *
 * @author Rohan Padhye
 */
public class Coverage implements TraceEventVisitor, ICoverage<Counter> {

    /** The size of the coverage map. */
    private final int COVERAGE_MAP_SIZE = (1 << 16) - 1; // Minus one to reduce collisions

    /** The coverage counts for each edge. */
    private final Counter counter = new NonZeroCachingCounter(COVERAGE_MAP_SIZE);

    /** Creates a new coverage map. */
    public Coverage() {

    }

    /**
     * Creates a copy of an this coverage map.
     *
     */
    public Coverage copy() {
        Coverage ret = new Coverage();
        for (int idx = 0; idx < COVERAGE_MAP_SIZE; idx++) {
            ret.counter.setAtIndex(idx, this.counter.getAtIndex(idx));
        }
        return ret;
    }

    /**
     * Returns the size of the coverage map.
     *
     * @return the size of the coverage map
     */
    @Override
    public int size() {
        return COVERAGE_MAP_SIZE;
    }

    /**
     * Updates coverage information based on emitted event.
     *
     * <p>This method updates its internal counters for branch and
     * call events.</p>
     *
     * @param e the event to be processed
     */
    public void handleEvent(TraceEvent e) {
        e.applyVisitor(this);
    }

    @Override
    public void visitBranchEvent(BranchEvent b) {
        counter.increment1(b.getIid(), b.getArm());
    }

    @Override
    public void visitCallEvent(CallEvent e) {
        counter.increment(e.getIid());
    }

    /**
     * Returns the number of edges covered.
     *
     * @return the number of edges with non-zero counts
     */
    @Override
    public int getNonZeroCount() {
        return counter.getNonZeroSize();
    }

    /**
     * Returns a collection of branches that are covered.
     *
     * @return a collection of keys that are covered
     */
    @Override
    public IntList getCovered() {
        return counter.getNonZeroIndices();
    }

    /**
     * Returns a set of edges in this coverage that don't exist in baseline
     *
     * @param baseline the baseline coverage
     * @return the set of edges that do not exist in {@code baseline}
     */
    @Override
    public IntList computeNewCoverage(ICoverage baseline) {
        IntArrayList newCoverage = new IntArrayList();

        IntList baseNonZero = this.counter.getNonZeroIndices();
        IntIterator iter = baseNonZero.intIterator();
        while (iter.hasNext()) {
            int idx = iter.next();
            if (baseline.getCounter().getAtIndex(idx) == 0) {
                newCoverage.add(idx);
            }
        }
        return newCoverage;
    }

    /**
     * Clears the coverage map.
     */
    @Override
    public void clear() {
        this.counter.clear();
    }

    private static int[] HOB_CACHE = new int[1024];

    /* Computes the highest order bit */
    private static int computeHob(int num)
    {
        if (num == 0)
            return 0;

        int ret = 1;

        while ((num >>= 1) != 0)
            ret <<= 1;

        return ret;
    }

    /** Populates the HOB cache. */
    static {
        for (int i = 0; i < HOB_CACHE.length; i++) {
            HOB_CACHE[i] = computeHob(i);
        }
    }

    /** Returns the highest order bit (perhaps using the cache) */
    private static int hob(int num) {
        if (num < HOB_CACHE.length) {
            return HOB_CACHE[num];
        } else {
            return computeHob(num);
        }
    }


    /**
     * Updates this coverage with bits from the parameter.
     *
     * @param that the run coverage whose bits to OR
     *
     * @return <code>true</code> iff <code>that</code> is not a subset
     *         of <code>this</code>, causing <code>this</code> to change.
     */
    @Override
    public boolean updateBits(ICoverage that) {
        boolean changed = false;
        if (that.getCounter().hasNonZeros()) {
            for (int idx = 0; idx < COVERAGE_MAP_SIZE; idx++) {
                int before = this.counter.getAtIndex(idx);
                int after = before | hob(that.getCounter().getAtIndex(idx));
                if (after != before) {
                    this.counter.setAtIndex(idx, after);
                    changed = true;
                }
            }
        }
        return changed;
    }

    /** Returns a hash code of the edge counts in the coverage map. */
    @Override
    public int hashCode() {
        return Arrays.hashCode(counter.counts);
    }

    /**
     * Returns a hash code of the list of edges that have been covered at least once.
     *
     * @return a hash of non-zero entries
     */
    @Override
    public int nonZeroHashCode() {
        return counter.getNonZeroIndices().hashCode();
    }

    /**
     * @return a string representing the counter
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Coverage counts: \n");
        for (int i = 0; i < counter.counts.length; i++) {
            if (counter.counts[i] == 0) {
                continue;
            }
            sb.append(i);
            sb.append("->");
            sb.append(counter.counts[i]);
            sb.append('\n');
        }
        return sb.toString();
    }

    @Override
    public Counter getCounter() {
        return counter;
    }
}
