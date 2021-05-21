package edu.berkeley.cs.jqf.instrument.mutation;

public class MutationTimeoutException extends Exception {
    private static final int MAX_ITERATIONS = 100000;
    private static int jumps = 0;

    public static void resetTimeout() {
        jumps = 0;
    }

    public static void checkTimeout() throws Exception {
        if(++jumps > MAX_ITERATIONS) {
            throw new MutationTimeoutException();
        }
    }
}
