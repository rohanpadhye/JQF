package edu.berkeley.cs.jqf.fuzz.difffuzz;

import edu.berkeley.cs.jqf.fuzz.random.NoGuidance;

import java.io.PrintStream;
import java.lang.reflect.Method;

public class DiffFuzzNoGuidance extends NoGuidance implements DiffFuzzGuidance {
    public DiffFuzzNoGuidance(long maxTrials, PrintStream out) {
        super(maxTrials, out);
    }

    @Override
    public void setCompare(Method m) {}
}
