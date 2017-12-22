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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReturnEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;

/**
 * @author Rohan Padhye
 */
public class ExecutionIndexingState {
    private final int COUNTER_SIZE = 6151;

    private int depth = 0;
    private Deque<Counter> stackOfCounters = new ArrayDeque<>();
    private Deque<Integer> rollingIndex = new ArrayDeque<>();

    public ExecutionIndexingState() {
        // Create a counter for depth = 0
        stackOfCounters.push(new Counter(COUNTER_SIZE));
    }

    public void pushCall(CallEvent e) {
        // Increment counter for call-site
        int count = stackOfCounters.peek().increment(e.getIid());

        // Increment depth
        depth++;

        // Add to rolling execution index
        rollingIndex.push(e.getIid());
        rollingIndex.push(count);

        // Push a new counter for this new prefix
        stackOfCounters.push(new Counter(COUNTER_SIZE));

    }

    public void popReturn(ReturnEvent e) {
        // Decrement depth
        depth--;

        // Pop twice from rolling index
        rollingIndex.pop();
        rollingIndex.pop();

        // Pop counter
        stackOfCounters.pop();
    }

    public ExecutionIndex getExecutionIndex(TraceEvent e) {
        // Increment counter for event
        int count = stackOfCounters.peek().increment(e.getIid());

        // Add to rolling execution index
        rollingIndex.push(e.getIid());
        rollingIndex.push(count);

        // Snapshot the rolling index
        int[] ei = new int[rollingIndex.size()];
        assert (ei.length == (depth + 1) * 2);

        // Copy stack backwards into integer array
        Iterator<Integer> it = rollingIndex.descendingIterator();
        int i = 0;
        while (it.hasNext()) {
            ei[i++] = it.next();
        }
        assert (i == ei.length);

        // Pop twice from rolling index
        rollingIndex.pop();
        rollingIndex.pop();

        return new ExecutionIndex(ei);
    }
}
