package edu.berkeley.cs.jqf.instrument.mutation;

import java.util.ArrayList;

/**
 * This is a class with static methods that are invoked from
 * instrumentation in mutated test methods.
 *
 * {@see {@link edu.berkeley.cs.jqf.instrument.tracing.SingleSnoop}}
 *
 */
public class MutationSnoop {

    public static final long TIMEOUT_TICKS = Integer.getInteger("jqf.mutation.TIMEOUT_TICKS", 100_0000);

    /**
     * Validates whether or not a timeout has occurred.
     *
     * This method is invoked periodically from mutated test classes,
     * in order to keep track of time. Each invocation of this method
     * increments a counter.
     *
     * @param id the unique identifier of the mutation instance
     * @throws MutationTimeoutException
     */
    public static void checkTimeout(int id) throws MutationTimeoutException {
        if (MutationInstance.getInstance(id).incrementTimeoutCounter() > TIMEOUT_TICKS) {
            throw new MutationTimeoutException(MutationInstance.getInstance(id).getTimeoutCounter());
        }
    }

}
