/*
 * Copyright (c) 2017-2018 The Regents of the University of California
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.berkeley.cs.jqf.fuzz.junit;

import java.io.PrintStream;

import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.instrument.tracing.SingleSnoop;
import edu.berkeley.cs.jqf.instrument.tracing.TraceLogger;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;

public class GuidedFuzzing {

    private static Guidance guidance;

    public static long DEFAULT_MAX_TRIALS = 100;

    /**
     * Sets the current global fuzzing guidance.
     *
     * Note: There can only be one guidance in any given JVM,
     * because the instrumented test classes make static method
     * calls to generate callback events.
     *
     * The fuzzing entry point (i.e., the target method being fuzzed)
     * should be invoked in the same thread as the thread that sets
     * the global guidance. This property is ensured by all variants
     * of {@link GuidedFuzzing#run(Class, String, Guidance, PrintStream)}.
     *
     * @param g the guidance instance
     * @throws IllegalStateException if a guidance has already been set
     */
    public static synchronized void setGuidance(Guidance g) {
        if (guidance != null) {
            throw new IllegalStateException("Cannot set more than one guidance simultaneously");
        }
        guidance = g;
    }

    /**
     * Returns the currently registered Guidance instance.
     *
     * @return the currently registered Guidance instance
     */
    public static Guidance getCurrentGuidance() {
        return guidance;
    }

    /**
     * Unsets the current global fuzzing guidance.
     *
     * This allows running multiple fuzzing sessions in the same
     * JVM instance sequentially. This method should be invoked
     * from the same thread that last invoked {@link #setGuidance(Guidance)}.
     * This method removes any tracers associated with the
     * current thread, so that the entry point can be detected again.
     * This property is ensured by {@link GuidedFuzzing#run(Class, String, Guidance, PrintStream)}.
     *
     */
    public static synchronized void unsetGuidance() {
        guidance = null;
        TraceLogger.get().remove();
    }


    /**
     * Runs the guided fuzzing loop, using the system class loader to load
     * test-application classes.
     *
     * <p>The test class must be annotated with <code>@RunWith(JQF.class)</code>
     * and the test method must be annotated with <code>@Fuzz</code>.</p>
     *
     * <p>Once this method is invoked, the guided fuzzing loop runs continuously
     * until the guidance instance decides to stop by returning <code>false</code>
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
     * @return the Junit-style test result
     */
    public synchronized static Result run(String testClassName, String testMethod,
                                        Guidance guidance, PrintStream out) throws ClassNotFoundException, IllegalStateException {

        // Run with the system class loader
        return run(testClassName, testMethod, ClassLoader.getSystemClassLoader(), guidance, out);
    }

    /**
     * Runs the guided fuzzing loop, using a provided classloader to load
     * test-application classes.
     *
     * <p>The test class must be annotated with <code>@RunWith(JQF.class)</code>
     * and the test method must be annotated with <code>@Fuzz</code>.</p>
     *
     * <p>Once this method is invoked, the guided fuzzing loop runs continuously
     * until the guidance instance decides to stop by returning <code>false</code>
     * for {@link Guidance#hasInput()}. Until the fuzzing stops, this method
     * cannot be invoked again (i.e. at most one guided fuzzing can be running
     * at any time in a single JVM instance).</p>
     *
     * @param testClassName the test class containing the test method
     * @param testMethod    the test method to execute in the fuzzing loop
     * @param loader        the classloader to load the test class with
     * @param guidance      the fuzzing guidance
     * @param out           an output stream to log Junit messages
     * @throws ClassNotFoundException if testClassName cannot be loaded
     * @throws IllegalStateException if a guided fuzzing run is currently executing
     * @return the Junit-style test result
     */
    public synchronized static Result run(String testClassName, String testMethod,
                                        ClassLoader loader,
                                        Guidance guidance, PrintStream out) throws ClassNotFoundException, IllegalStateException {
        // Set the given classloader as the thread's context class loader,
        // so that applications that use this API can still find test-classes
        Thread.currentThread().setContextClassLoader(loader);

        // Load the application class using the provided class loader
        Class<?> testClass =
                java.lang.Class.forName(testClassName, true, loader);

        return run(testClass, testMethod, guidance, out);
    }


    /**
     * Runs the guided fuzzing loop for a resolved class.
     *
     * <p>The test class must be annotated with <code>@RunWith(JQF.class)</code>
     * and the test method must be annotated with <code>@Fuzz</code>.</p>
     *
     * <p>Once this method is invoked, the guided fuzzing loop runs continuously
     * until the guidance instance decides to stop by returning <code>false</code>
     * for {@link Guidance#hasInput()}. Until the fuzzing stops, this method
     * cannot be invoked again (i.e. at most one guided fuzzing can be running
     * at any time in a single JVM instance).</p>
     *
     * @param testClass     the test class containing the test method
     * @param testMethod    the test method to execute in the fuzzing loop
     * @param guidance      the fuzzing guidance
     * @param out           an output stream to log Junit messages
     * @throws IllegalStateException if a guided fuzzing run is currently executing
     * @return the Junit-style test result
     */
    public synchronized static Result run(Class<?> testClass, String testMethod,
                                          Guidance guidance, PrintStream out) throws IllegalStateException {

        // Ensure that the class uses the right test runner
        RunWith annotation = testClass.getAnnotation(RunWith.class);
        if (annotation == null || !(JQF.class.isAssignableFrom(annotation.value()))) {
            throw new IllegalArgumentException(testClass.getName() + " is not annotated with @RunWith(JQF.class)");
        }

        try {
            // Set the static guidance instance
            setGuidance(guidance);

            // Register callback
            SingleSnoop.setCallbackGenerator(guidance::generateCallBack);

            // Create a JUnit Request
            Request testRequest = Request.method(testClass, testMethod);

            // Instantiate a runner (may return an error)
            Runner testRunner = testRequest.getRunner();

            // Start tracing for the test method
            SingleSnoop.startSnooping(testClass.getName() + "#" + testMethod);

            // Run the test
            JUnitCore junit = new JUnitCore();
            if (out != null) {
                junit.addListener(new TextListener(out));
            }

            return junit.run(testRunner);

        } finally {
            // Make sure to de-register the guidance before returning
            unsetGuidance();
        }



    }

}
