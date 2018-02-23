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

import edu.berkeley.cs.jqf.instrument.tracing.events.BranchEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEventVisitor;

/**
 * Utility class to collect branch and function coverage
 *
 * @author Rohan Padhye
 */
public class Coverage implements TraceEventVisitor {

    /** The size of the coverage map. */
    private final int COVERAGE_MAP_SIZE = (1 << 16) - 1; // Minus one to reduce collisions

    /** The coverage counts for each edge. */
    private final Counter counter = new Counter(COVERAGE_MAP_SIZE);

    /** Creates a new coverage map. */
    public Coverage() {

    }

    /**
     * Creates a copy of an existing coverage map.
     *
     * @param that the coverage map to copy
     */
    public Coverage(Coverage that) {
        int[] thisCounts = this.counter.getCounts();
        int[] thatCounts = that.counter.getCounts();
        for (int i = 0; i < COVERAGE_MAP_SIZE; i++) {
            thisCounts[i] = thatCounts[i];
        }
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
        counter.increment(b.getIid() * 31 + b.getArm());
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
    public int getNonZeroCount() {
        return counter.nonZeroSize();
    }

    /**
     * Returns a collection of branches that are covered.
     *
     * @return a collection of keys that are covered
     */
    public Collection<?> getCovered() {
        return counter.getNonZeroIndices();
    }


    public Collection<?> computeNewCoverage(Coverage baseline) {
        Collection<Integer> newCoverage = new ArrayList<>();
        int[] thisCounts = this.counter.getCounts();
        int[] thatCounts = baseline.counter.getCounts();
        for (int i = 0; i < COVERAGE_MAP_SIZE; i++) {
            if (thisCounts[i] > 0 && thatCounts[i] == 0) {
                newCoverage.add(i);
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


    public boolean updateMax(Coverage cov) {
        boolean changed = false;
        int[] thisCounts = this.counter.getCounts();
        int[] thatCounts = cov.counter.getCounts();
        for (int i = 0; i < COVERAGE_MAP_SIZE; i++) {
            if (thatCounts[i] > thisCounts[i]) {
                thisCounts[i] = thatCounts[i];
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Updates this coverage with bits from the parameter.
     *
     * @param that the run coverage whose bits to OR
     *
     * @return <tt>true</tt> iff <tt>that</tt> is not a subset
     *         of <tt>this</tt>, causing <tt>this</tt> to change.
     */
    public boolean updateBits(Coverage that) {
        boolean changed = false;
        int[] thisCounts = this.counter.getCounts();
        int[] thatCounts = that.counter.getCounts();
        for (int i = 0; i < COVERAGE_MAP_SIZE; i++) {
            int before = thisCounts[i];
            thisCounts[i] |= thatCounts[i];
            if (thisCounts[i] != before) {
                changed = true;
            }
        }
        return changed;
    }


}
