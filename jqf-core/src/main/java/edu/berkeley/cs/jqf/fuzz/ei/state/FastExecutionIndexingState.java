package edu.berkeley.cs.jqf.fuzz.ei.state;

import janala.instrument.FastCoverageListener;

// This logic is similar to FastNonCollidingCoverage.
// We should consider merging them. Note we only
// track EI in main thread to avoid non-determinism.
public class FastExecutionIndexingState extends AbstractExecutionIndexingState implements FastCoverageListener {
    @Override
    public void logMethodBegin(int iid) {
        if (Thread.currentThread().getId() == 1) {
            setLastEventIid(iid);
            pushCall(iid);
        }
    }

    @Override
    public void logMethodEnd(int iid) {
        if (Thread.currentThread().getId() == 1) {
            setLastEventIid(iid);
            popReturn(iid);
        }
    }

    @Override
    public void logJump(int iid, int branch) {
        if (Thread.currentThread().getId() == 1)  {
            setLastEventIid(iid + branch);
        }
    }

    @Override
    public void logLookUpSwitch(int value, int iid, int dflt, int[] cases) {
        // Compute arm index or else default
        if (Thread.currentThread().getId() == 1) {
            int arm = cases.length;
            for (int i = 0; i < cases.length; i++) {
                if (value == cases[i]) {
                    arm = i;
                    break;
                }
            }
            arm++;
            setLastEventIid(iid + arm);
        }
    }

    @Override
    public void logTableSwitch(int value, int iid, int min, int max, int dflt) {
        if (Thread.currentThread().getId() == 1) {
            int arm = 1 + max - min;
            if (value >= min && value <= max) {
                arm = value - min;
            }
            arm++;
            setLastEventIid(iid + arm);
        }
    }
}
