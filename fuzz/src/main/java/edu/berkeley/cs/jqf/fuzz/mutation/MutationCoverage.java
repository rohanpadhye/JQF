package edu.berkeley.cs.jqf.fuzz.mutation;

import edu.berkeley.cs.jqf.fuzz.util.Coverage;
import edu.berkeley.cs.jqf.instrument.tracing.events.KillEvent;
import mutation.MutationInstance;

import java.util.HashSet;
import java.util.Set;

public class MutationCoverage extends Coverage {
    //TODO subclass instead of modify
    private Set<MutationInstance> caughtMutants = new HashSet<>();
    private Set<MutationInstance> seenMutants = new HashSet<>();

    @Override
    public void visitKillEvent(KillEvent k) {
        caughtMutants.add(k.getMutant());
    }

    @Override
    public void clear() {
        super.clear();
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
