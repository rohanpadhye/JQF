package edu.berkeley.cs.jqf.instrument.tracing;

import janala.instrument.FastCoverageListener;

public class FastCoverageSnoop {
    static FastCoverageListener coverageListener = new FastCoverageListener() {
        @Override
        public void logCoverage(int iid, int arm) {

        }
    };

    @SuppressWarnings("unused") //Invoked by instrumentation
    public static void LOGJUMP(int iid, int branch) {
        coverageListener.logCoverage(iid, branch);
    }

    @SuppressWarnings("unused") //Invoked by instrumentation
    public static void LOGLOOKUPSWITCH(int value, int iid, int dflt, int[] cases) {
        // Compute arm index or else default
        int arm = cases.length;
        for (int i = 0; i < cases.length; i++) {
            if (value == cases[i]) {
                arm = i;
                break;
            }
        }
        arm++;
        coverageListener.logCoverage(iid, arm);
    }

    @SuppressWarnings("unused") //Invoked by instrumentation
    public static void LOGTABLESWITCH(int value, int iid, int min, int max, int dflt) {
        int arm = 1 + max - min;
        if (value >= min && value <= max) {
            arm = value - min;
        }
        arm++;
        coverageListener.logCoverage(iid, arm);
    }

    public static void setFastCoverageListener(FastCoverageListener runCoverage) {
        coverageListener = runCoverage;
    }
}
