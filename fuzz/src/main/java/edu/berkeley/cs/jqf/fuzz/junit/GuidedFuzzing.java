/*
 The MIT License

 Copyright (c) 2017 University of California, Berkeley

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package edu.berkeley.cs.jqf.fuzz.junit;

import java.io.PrintStream;

import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.NoGuidance;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.JQF;
import edu.berkeley.cs.jqf.instrument.tracing.SingleSnoop;
import org.junit.internal.TextListener;
import org.junit.internal.runners.ErrorReportingRunner;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;

public class GuidedFuzzing {

    private static Guidance guidance;

    private static long DEFAULT_MAX_TRIALS = 100;

    private static void setGuidance(Guidance g) {
        if (guidance != null) {
            throw new IllegalStateException("Can only set guided once.");
        }
        guidance = g;
    }

    /**
     * Returns the currently registered Guidance instance.
     *
     * <p>If no guidance is set, this method creates a new
     * instance of {@link NoGuidance}, registers it, and returns
     * a reference to it.</p>
     *
     * @return the currently registered Guidance instance
     */
    public static Guidance getCurrentGuidance() {
        if (guidance == null) {
            System.err.println(String.format("Warning: No guidance set. " +
                    " Falling back to default %d trials with no feedback", DEFAULT_MAX_TRIALS));
            setGuidance(new NoGuidance(DEFAULT_MAX_TRIALS, System.err));
        }

        return guidance;
    }

    private static void unsetGuidance() {
        guidance = null;
    }


    /**
     * Runs the guided fuzzing loop.
     *
     * <p>The test class must be annotated with <tt>@RunWith(JQF.class)</tt>
     * and the test method must be annotated with <tt>@Fuzz</tt>.</p>
     *
     * <p>Once this method is invoked, the guided fuzzing loop runs continuously
     * until the guidance instance decides to stop by returning <tt>false</tt>
     * for {@link Guidance#hasInput()}. Until the fuzzing stops, this method
     * cannot be invoked again (i.e. at most one guided fuzzing can be running
     * at any time in a single JVM instance).</p>
     *
     * @param testClassName the test class containing the test method
     * @param testMethod    the test method to execute in the fuzzing loop
     * @param guidance      the fuzzing guidance
     * @param out           an output stream to log Junit messages
     * @throws ClassNotFoundException if testClassName cannot be loaded
     * @throws IllegalStateException if a guided fuzzing run is currently executing
     */
    public synchronized static void run(String testClassName, String testMethod,
                                        Guidance guidance, PrintStream out) throws ClassNotFoundException, IllegalStateException {
        Class<?> testClass =
        java.lang.Class.forName(testClassName, true, ClassLoader.getSystemClassLoader());

        run(testClass, testMethod, guidance, out);

    }


    /**
     * Runs the guided fuzzing loop.
     *
     * <p>The test class must be annotated with <tt>@RunWith(JQF.class)</tt>
     * and the test method must be annotated with <tt>@Fuzz</tt>.</p>
     *
     * <p>Once this method is invoked, the guided fuzzing loop runs continuously
     * until the guidance instance decides to stop by returning <tt>false</tt>
     * for {@link Guidance#hasInput()}. Until the fuzzing stops, this method
     * cannot be invoked again (i.e. at most one guided fuzzing can be running
     * at any time in a single JVM instance).</p>
     *
     * @param testClass     the test class containing the test method
     * @param testMethod    the test method to execute in the fuzzing loop
     * @param guidance      the fuzzing guidance
     * @param out           an output stream to log Junit messages
     * @throws IllegalStateException if a guided fuzzing run is currently executing
     */
    public synchronized static void run(Class<?> testClass, String testMethod,
                                        Guidance guidance, PrintStream out) throws IllegalStateException {

        // Ensure that the class uses the right test runner
        RunWith annotation = testClass.getAnnotation(RunWith.class);
        if (annotation == null || !annotation.value().equals(JQF.class)) {
            throw new IllegalArgumentException(testClass.getName() + " is not annotated with @RunWith(JQF.class)");
        }


        // Set the static guided instance
        setGuidance(guidance);

        // Register callback
        SingleSnoop.setCallbackGenerator(guidance::generateCallBack);

        // Create a JUnit Request
        Request testRequest = Request.method(testClass, testMethod);

        // Instantiate a runner (throws exception if testMethod is not a proper test method)
        Runner testRunner = testRequest.getRunner();

        // Start tracing for the test method
        SingleSnoop.startSnooping(testClass.getName() + "#" + testMethod);

        // Run the test and make sure to de-register the guidance before returning
        try {
            JUnitCore junit = new JUnitCore();
            if (out != null) {
                junit.addListener(new TextListener(out));
            }
            junit.run(testRunner);
        } finally {
            unsetGuidance();
        }


        if (testRunner instanceof ErrorReportingRunner) {
            throw new IllegalArgumentException(String.format("Could not instantiate a Junit runner for method %s#%s.", testClass, testMethod));
        }

    }

}
