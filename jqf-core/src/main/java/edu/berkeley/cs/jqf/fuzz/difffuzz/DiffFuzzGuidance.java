package edu.berkeley.cs.jqf.fuzz.difffuzz;

import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;

import java.lang.reflect.Method;

public interface DiffFuzzGuidance extends Guidance {
    void setCompare(Method m);

    /**
     * Accepts the outcome of one trial captured by {@link DiffTrialExecutor}.
     *
     * <p>The default rethrows whatever the trial threw, so an exception counts as
     * a failure. {@link DiffFuzzReproGuidance} overrides this to compare the
     * outcome against a reference run instead.
     *
     * @param outcome the captured return value or thrown error
     * @throws Throwable to fail the trial: a rethrown error or a {@link DiffException}
     */
    default void acceptOutcome(Outcome outcome) throws Throwable {
        if (outcome.thrown != null) {
            throw outcome.thrown;
        }
    }
}
