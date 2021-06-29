/*
 * Copyright (c) 2021 Isabella Laybourn, Rohan Padhye
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
package edu.berkeley.cs.jqf.instrument.mutation;

import java.util.ArrayList;

public class MutationInstance {

    /** Globally unique identifier for this mutation instance */
    public final int id;

    /** Static list of all registered mutation instances. */
    private static final ArrayList<MutationInstance> mutationInstances = new ArrayList<>();

    /** The type of mutation represented by a mutator */
    final Mutator mutator;

    /** Name of the class to mutate */
    final String className;

    /** Numbered instance of the opportunity for mutation this classloader uses */
    final long mutatorOffsetWithinClass;

    /**
     * Counter that is incremented during execution of this mutation instance to
     * catch infinite loops.
     */
    private long timeoutCounter = 0;

    // TODO potential for more information:
    // line number
    // who's seen it
    // whether this mutation is likely to be killed by a particular input

    public MutationInstance(Mutator m, long i, String n) {
        this.id = mutationInstances.size();
        this.className = n;
        this.mutator = m;
        this.mutatorOffsetWithinClass = i;

        // Register mutation instance
        mutationInstances.add(this);
    }

    public void resetTimer() {
        this.timeoutCounter = 0;
    }

    public long getTimeoutCounter() {
        return this.timeoutCounter;
    }

    public long incrementTimeoutCounter() {
        return ++this.timeoutCounter;
    }

    @Override
    public String toString() {
        return String.format("%s::%s::%d", className, mutator, mutatorOffsetWithinClass);
    }

    public static MutationInstance getInstance(int id) {
        return mutationInstances.get(id);
    }

    public static int getNumInstances() {
        return mutationInstances.size();
    }

    @Override
    public boolean equals(Object that) {
        // Mutation instances are globally unique
        return this == that;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
