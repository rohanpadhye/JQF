package edu.berkeley.cs.jqf.fuzz.difffuzz;

public class DiffException extends RuntimeException {
    Outcome out1, out2;
    public DiffException(Outcome o1, Outcome o2) {
        super("diff between \"" + o1 + "\" and \"" + o2 + "\"");
        out1 = o1;
        out2 = o2;
    }
}
