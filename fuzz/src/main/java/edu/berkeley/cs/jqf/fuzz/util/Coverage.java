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

import edu.berkeley.cs.jqf.instrument.tracing.events.BranchEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;

/**
 * Utility class to collect branch and function coverage
 *
 * @author Rohan Padhye
 */
public class Coverage {

    /** The size of the coverage map. */
    private final int COVERAGE_MAP_SIZE = 1 << 16;

    /** The coverage counts for each edge. */
    private final Counter counter = new Counter(COVERAGE_MAP_SIZE);

    /**
     * Updates coverage information based on emitted event.
     *
     * <p>This method updates its internal counters for branch and
     * call events.</p>
     *
     * @param e the event to be processed
     */
    public void handleEvent(TraceEvent e) {
        // Handle branches and calls
        if (e instanceof BranchEvent) {
            BranchEvent b = (BranchEvent) e;
            counter.increment(b.getIid() * 31 + b.getArm());
        } else if (e instanceof CallEvent) {
            counter.increment(e.getIid());
        }
    }

    /**
     * Get the number of edges covered.
     *
     * @return the number of edges with non-zero counts
     */
    public int getNonZeroCount() {
        return counter.nonZeroValues().size();
    }

    /**
     * Clears the coverage map.
     */
    public void clear() {
        this.counter.clear();
    }


}
