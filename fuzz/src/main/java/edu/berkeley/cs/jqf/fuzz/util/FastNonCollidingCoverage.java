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

import edu.berkeley.cs.jqf.instrument.tracing.events.BranchEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import janala.instrument.FastCoverageListener;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.api.tuple.primitive.IntIntPair;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * Utility class to collect branch and function coverage
 *
 * @author Jonathan Bell
 */
public class FastNonCollidingCoverage extends FastCoverageListener.Default implements ICoverage<FastNonCollidingCounter> {

    /** The starting size of the coverage map. */
    private final int COVERAGE_MAP_SIZE = (1 << 8);

    private final FastNonCollidingCounter counter = new FastNonCollidingCounter(COVERAGE_MAP_SIZE);

    /** Creates a new coverage map. */
    public FastNonCollidingCoverage() {

    }

    /**
     * Creates a copy of an existing coverage map.
     *
     */
    public FastNonCollidingCoverage copy() {
        FastNonCollidingCoverage ret = new FastNonCollidingCoverage();
        ret.counter.copyFrom(this.counter);
        return ret;
    }

    /**
     * Returns the size of the coverage map.
     *
     * @return the size of the coverage map
     */
    public int size() {
        return COVERAGE_MAP_SIZE;
    }

    /**
     * Returns the number of edges covered.
     *
     * @return the number of edges with non-zero counts
     */
    public int getNonZeroCount() {
        return counter.getNonZeroSize();
    }

    /**
     * Returns a collection of branches that are covered.
     *
     * @return a collection of keys that are covered
     */
    public IntList getCovered() {
        return counter.getNonZeroIndices();
    }

    /**
     * Returns a set of edges in this coverage that don't exist in baseline
     *
     * @param baseline the baseline coverage
     * @return the set of edges that do not exist in {@code baseline}
     */
    public IntList computeNewCoverage(ICoverage baseline) {
        IntArrayList newCoverage = new IntArrayList();

        IntList baseNonZero = this.counter.getNonZeroKeys();
        IntIterator iter = baseNonZero.intIterator();
        while (iter.hasNext()) {
            int idx = iter.next();
            if (baseline.getCounter().get(idx) == 0) {
                newCoverage.add(idx);
            }
        }
        return newCoverage;
    }

    /**
     * Clears the coverage map.
     */
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
    public boolean updateBits(ICoverage that) {
        boolean changed = false;
        synchronized (this.counter){
            synchronized (that.getCounter()){
                FastNonCollidingCounter thatCounter = (FastNonCollidingCounter) that.getCounter();
                Iterator<IntIntPair> thatIter = thatCounter.counts.keyValuesView().iterator();

                while(thatIter.hasNext()){
                    IntIntPair coverageEntry = thatIter.next();
                    int before = this.counter.counts.get(coverageEntry.getOne());
                    int after = before | hob(coverageEntry.getTwo());
                    if(after != before){
                        this.counter.counts.put(coverageEntry.getOne(), after);
                        changed = true;
                    }
                    if(before == 0){
                        this.counter.nonZeroKeys.add(coverageEntry.getOne());
                    }
                }
            }
        }
        return changed;
    }

    /** Returns a hash code of the edge counts in the coverage map. */
    @Override
    public int hashCode() {
        return counter.counts.hashCode();
    }

    /**
     * Returns a hash code of the list of edges that have been covered at least once.
     *
     * @return a hash of non-zero entries
     */
    public int nonZeroHashCode() {
        return counter.getNonZeroIndices().hashCode();
    }

    @Override
    public Counter getCounter() {
        return this.counter;
    }

    /**
     * @return a string representing the counter
     */
    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Coverage counts: \n");
        for (int i = 0; i < counter.counts.size(); i++) {
            if (counter.counts.get(i) == 0) {
                continue;
            }
            sb.append(i);
            sb.append("->");
            sb.append(counter.counts.get(i));
            sb.append('\n');
        }
        return sb.toString();
    }


    @Override
    public void logMethodBegin(int iid) {
        logCoverage(iid, 0);
    }

    @Override
    public void logJump(int iid, int branch) {
        logCoverage(iid, branch);
    }

    @Override
    public void logLookUpSwitch(int value, int iid, int dflt, int[] cases) {
        // Compute arm index or else default
        int arm = cases.length;
        for (int i = 0; i < cases.length; i++) {
            if (value == cases[i]) {
                arm = i;
                break;
            }
        }
        arm++;
        logCoverage(iid, arm);
    }

    @Override
    public void logTableSwitch(int value, int iid, int min, int max, int dflt) {
        int arm = 1 + max - min;
        if (value >= min && value <= max) {
            arm = value - min;
        }
        arm++;
        logCoverage(iid, arm);
    }

    private void logCoverage(int iid, int arm) {
        counter.increment(iid + arm);
    }
}
