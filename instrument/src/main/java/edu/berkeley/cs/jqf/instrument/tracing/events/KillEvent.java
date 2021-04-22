package edu.berkeley.cs.jqf.instrument.tracing.events;

import janala.logger.inst.MemberRef;
import mutation.MutationInstance;

public class KillEvent extends TraceEvent {
    private MutationInstance mutant;

    public KillEvent(int iid, MemberRef method, int lineNumber) {
        super(iid, method, lineNumber);
    }

    public KillEvent(int iid, MemberRef method, int lineNumber, MutationInstance mutationInstance) {
        super(iid, method, lineNumber);
        mutant = mutationInstance;
    }

    public MutationInstance getMutant() {
        return mutant;
    }

    @Override
    public void applyVisitor(TraceEventVisitor v) {
        v.visitKillEvent(this);
    }
}
