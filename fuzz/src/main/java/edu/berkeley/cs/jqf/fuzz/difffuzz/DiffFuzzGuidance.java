package edu.berkeley.cs.jqf.fuzz.difffuzz;

import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.junit.TrialRunner;
import edu.berkeley.cs.jqf.instrument.InstrumentationException;
import org.junit.runners.model.FrameworkMethod;

import java.lang.reflect.Method;

public interface DiffFuzzGuidance extends Guidance {
    void setCompare(Method m);

    /** common utility method for use in run */
    default Outcome getOutcome(Class<?> clazz, FrameworkMethod method, Object[] args) {
        try {
            TrialRunner dtr = new TrialRunner(clazz, method, args);
            dtr.run();
            return new Outcome(dtr.getOutput(), null);
        } catch(InstrumentationException e) {
            throw new GuidanceException(e);
        } catch (GuidanceException e) {
            throw e;
        } catch(Throwable e) {
            return new Outcome(null, e);
        }
    }
}
