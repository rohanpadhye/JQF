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
package edu.berkeley.cs.jqf.fuzz.ei;

import java.util.ArrayList;
import java.util.Arrays;

import edu.berkeley.cs.jqf.fuzz.util.Counter;
import edu.berkeley.cs.jqf.fuzz.util.NonZeroCachingCounter;
import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReturnEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEventVisitor;

/**
 * A mutable state representing the current call stack with prefix counts,
 * used to compute light-weight execution indexes.
 *
 * @see ExecutionIndex
 *
 * @author Rohan Padhye
 */
public class ExecutionIndexingState implements TraceEventVisitor {
    private final int COUNTER_SIZE = 6151;
    private final int MAX_SUPPORTED_DEPTH = 1024; // Nothing deeper than this

    private int depth = 0;
    private ArrayList<Counter> stackOfCounters = new ArrayList<>();
    private int[] rollingIndex = new int[2*MAX_SUPPORTED_DEPTH];

    public ExecutionIndexingState() {
        // Create a counter for depth = 0
        stackOfCounters.add(new NonZeroCachingCounter(COUNTER_SIZE));
    }

    public void pushCall(CallEvent e) {
        // Increment counter for call-site (note: this is subject to hash collisions)
        int count = stackOfCounters.get(depth).increment(e.getIid());

        // Add to rolling execution index
        rollingIndex[2*depth] = e.getIid();
        rollingIndex[2*depth + 1] = count;

        // Increment depth
        depth++;

        // Ensure that we do not go very deep
        if (depth >= MAX_SUPPORTED_DEPTH) {
            throw new StackOverflowError("Very deep stack; cannot compute execution index");
        }

        // Push a new counter if it does not exist
        if (depth >= stackOfCounters.size()) {
            stackOfCounters.add(new NonZeroCachingCounter(COUNTER_SIZE));
        }

    }

    public void popReturn(ReturnEvent e) {
        // Clear the top-of-stack
        stackOfCounters.get(depth).clear();

        // Decrement depth
        depth--;

        assert (depth >= 0);
    }

    public ExecutionIndex getExecutionIndex(TraceEvent e) {
        // Increment counter for event (note: this is subject to hash collisions)
        int count = stackOfCounters.get(depth).increment(e.getIid());

        // Add to rolling execution index
        rollingIndex[2*depth] = e.getIid();
        rollingIndex[2*depth + 1] = count;

        // Snapshot the rolling index
        int size = 2*(depth+1); // 2 integers for each depth value
        int[] ei = Arrays.copyOf(rollingIndex, size);

        // Create an execution index
        return new ExecutionIndex(ei);
    }

    @Override
    public void visitCallEvent(CallEvent c) {
        this.pushCall(c);
    }

    @Override
    public void visitReturnEvent(ReturnEvent r) {
        this.popReturn(r);
    }
}
