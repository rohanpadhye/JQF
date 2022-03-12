package edu.berkeley.cs.jqf.instrument.tracing;

import janala.instrument.FastCoverageListener;

public class FastCoverageSnoop {
    static FastCoverageListener coverageListener = new FastCoverageListener.Default();

    @SuppressWarnings("unused") //Invoked by instrumentation
    public static void LOGMETHODBEGIN(int iid) {
        coverageListener.logMethodBegin(iid);
    }

    public static void LOGMETHODEND(int iid) {
        coverageListener.logMethodEnd(iid);
    }

    @SuppressWarnings("unused") //Invoked by instrumentation
    public static void LOGJUMP(int iid, int branch) {
        coverageListener.logJump(iid, branch);
    }

    @SuppressWarnings("unused") //Invoked by instrumentation
    public static void LOGLOOKUPSWITCH(int value, int iid, int dflt, int[] cases) {
        coverageListener.logLookUpSwitch(value, iid, dflt, cases);
    }

    @SuppressWarnings("unused") //Invoked by instrumentation
    public static void LOGTABLESWITCH(int value, int iid, int min, int max, int dflt) {
        coverageListener.logTableSwitch(value, iid, min, max, dflt);
    }

    public static void setFastCoverageListener(FastCoverageListener runCoverage) {
        coverageListener = runCoverage;
    }
}
