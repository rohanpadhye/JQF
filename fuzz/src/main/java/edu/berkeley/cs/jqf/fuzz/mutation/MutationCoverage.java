package edu.berkeley.cs.jqf.fuzz.mutation;

import edu.berkeley.cs.jqf.fuzz.util.Coverage;
import edu.berkeley.cs.jqf.instrument.tracing.events.KillEvent;
import mutation.MutationInstance;

import java.util.HashSet;
import java.util.Set;

public class MutationCoverage extends Coverage {
    //TODO subclass instead of modify
    private Set<MutationInstance> caughtMutants = new HashSet<>();

    @Override
    public void visitKillEvent(KillEvent k) {
        caughtMutants.add(k.getMutant());
    }

    public int numCaughtMutants() {
        return caughtMutants.size();
    }

    public boolean updateMutants(MutationCoverage that) {
        int prevSize = caughtMutants.size();
        caughtMutants.addAll(that.caughtMutants);
        return caughtMutants.size() > prevSize;
    }

    public Set<Object> getMutants() {
        Set<Object> toReturn = new HashSet<>(caughtMutants);
        return toReturn;
    }
}
