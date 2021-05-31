/*
 * Copyright (c) 2021 Isabella Laybourn
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
package edu.berkeley.cs.jqf.fuzz.mutation;

import edu.berkeley.cs.jqf.fuzz.util.Coverage;
import edu.berkeley.cs.jqf.instrument.mutation.MutationInstance;
import edu.berkeley.cs.jqf.instrument.tracing.events.KillEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class to collect mutation coverage
 *
 * @author Bella Laybourn
 */
public class MutationCoverage extends Coverage {
    private Set<MutationInstance> caughtMutants = new HashSet<>();
    private Set<MutationInstance> seenMutants = new HashSet<>();

    @Override
    public void visitKillEvent(KillEvent k) {
        caughtMutants.add(k.getMutant());
    }

    @Override
    public void clear() {
        super.clear();
        clearMutants();
    }

    public void clearMutants() {
        caughtMutants = new HashSet<>();
    }

    public int numCaughtMutants() {
        return caughtMutants.size();
    }

    public boolean updateMutants(MutationCoverage that) {
        int prevSize = caughtMutants.size();
        caughtMutants.addAll(that.caughtMutants);
        seenMutants.addAll(that.seenMutants);
        return caughtMutants.size() > prevSize;
    }

    public void see(MutationInstance mcl) {
        seenMutants.add(mcl);
    }

    public int numSeenMutants() {
        return seenMutants.size();
    }

    public Set<Object> getMutants() {
        return new HashSet<>(caughtMutants);
    }
}
