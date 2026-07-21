/*
 * Copyright (c) 2026 Vladimir Sitnikov and JQF Contributors
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
package edu.berkeley.cs.jqf.plugin;

import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import edu.berkeley.cs.jqf.fuzz.FuzzResult;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGenerator;
import edu.berkeley.cs.jqf.junit5.FuzzTest;
import edu.berkeley.cs.jqf.junit5.JUnit5FuzzRunner;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * Runs a fuzzing campaign for one test method, picking the run path from how the
 * method is written.
 *
 * <p>The plugin supports two test styles. A JUnit 4 method is annotated
 * {@code @Fuzz} on a {@code @RunWith(JQF.class)} class and runs through
 * {@link GuidedFuzzing}, exactly as before. A JUnit 5 method is annotated
 * {@link FuzzTest @FuzzTest} and runs through {@link JUnit5FuzzRunner}, which
 * drives the framework-independent engine with the JUnit 5 trial executor. Both
 * paths take the same plugin-built {@link Guidance}, so {@code jqf:fuzz} and
 * {@code jqf:repro} behave the same way regardless of the style.
 *
 * <p>The framework is chosen explicitly here rather than through
 * {@code ServiceLoader<FuzzFramework>}: the plugin classpath carries both the
 * JUnit 4 and JUnit 5 providers, so a service lookup would be ambiguous. Selecting
 * by the detected annotation keeps the choice deterministic and leaves the JUnit 4
 * results unchanged.
 */
public final class FuzzTestDispatcher {

    private FuzzTestDispatcher() {
    }

    /** The supported test styles. */
    public enum TestFramework {
        /** A {@code @RunWith(JQF.class)} class with a {@code @Fuzz} method. */
        JUNIT4,
        /** A method annotated with {@link FuzzTest @FuzzTest}. */
        JUNIT5
    }

    /**
     * Detects the test style of a method by its annotations.
     *
     * <p>A method named {@code methodName} carrying {@link FuzzTest @FuzzTest} is
     * JUnit 5; everything else is treated as JUnit 4, so the legacy path stays the
     * default and reports its usual errors when the class is not a JQF test.
     *
     * @param testClass  the loaded test class
     * @param methodName the test method name
     * @return the detected style
     */
    public static TestFramework detect(Class<?> testClass, String methodName) {
        for (Method method : testClass.getMethods()) {
            if (method.getName().equals(methodName) && method.isAnnotationPresent(FuzzTest.class)) {
                return TestFramework.JUNIT5;
            }
        }
        return TestFramework.JUNIT4;
    }

    /**
     * Runs the campaign for one test method, dispatching on the detected style.
     *
     * @param loader        the (typically instrumenting) class loader for the test class
     * @param testClassName the fully qualified test class name
     * @param methodName    the test method name
     * @param guidance      the guidance built by the plugin (Zest, Zeal, or repro)
     * @param out           a stream for JUnit log output, or {@code null}
     * @return the normalised campaign outcome
     * @throws ClassNotFoundException if the test class cannot be loaded
     */
    public static Outcome run(ClassLoader loader, String testClassName, String methodName,
                              Guidance guidance, PrintStream out) throws ClassNotFoundException {
        // Load without initialising: detection only reads annotations, so no instrumented
        // code runs before snooping starts on either path.
        Class<?> testClass = loader.loadClass(testClassName);
        if (detect(testClass, methodName) == TestFramework.JUNIT5) {
            return runJUnit5(loader, testClass, methodName, guidance);
        }
        // JUnit 4: unchanged from the legacy plugin. GuidedFuzzing re-resolves the class
        // (returning the cached, already-instrumented one) and sets the context class loader.
        Result result = GuidedFuzzing.run(testClassName, methodName, loader, guidance, out);
        return Outcome.fromJUnit4Result(result);
    }

    private static Outcome runJUnit5(ClassLoader loader, Class<?> testClass, String methodName,
                                     Guidance guidance) {
        Method method = findFuzzTestMethod(testClass, methodName);
        // The argument-generator provider lives on the test classpath, and
        // ServiceLoader resolves it through the context class loader.
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(loader);
        try {
            ArgumentsGenerator generator = JUnit5FuzzRunner.resolveArguments(testClass, method);
            FuzzResult result = JUnit5FuzzRunner.fuzz(testClass, method, guidance, generator);
            return Outcome.fromFuzzResult(result);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    private static Method findFuzzTestMethod(Class<?> testClass, String methodName) {
        for (Method method : testClass.getMethods()) {
            if (method.getName().equals(methodName) && method.isAnnotationPresent(FuzzTest.class)) {
                return method;
            }
        }
        throw new IllegalArgumentException("No @FuzzTest method named '" + methodName
                + "' found in " + testClass.getName());
    }

    /**
     * A test-framework-independent campaign result.
     *
     * <p>It normalises the JUnit 4 {@link Result} and the engine's
     * {@link FuzzResult} so the plugin's pass/fail handling is the same on both
     * paths.
     */
    public static final class Outcome {

        private final List<Throwable> failures;

        private Outcome(List<Throwable> failures) {
            this.failures = failures;
        }

        /**
         * Wraps a JUnit 4 result, keeping each failure's underlying exception.
         *
         * @param result the JUnit 4 result
         * @return the normalised outcome
         */
        public static Outcome fromJUnit4Result(Result result) {
            List<Throwable> failures = new ArrayList<>();
            for (Failure failure : result.getFailures()) {
                failures.add(failure.getException());
            }
            return new Outcome(failures);
        }

        /**
         * Wraps an engine result.
         *
         * @param result the engine result
         * @return the normalised outcome
         */
        public static Outcome fromFuzzResult(FuzzResult result) {
            return new Outcome(new ArrayList<>(result.getFailures()));
        }

        /**
         * Reports whether the campaign found no failing inputs.
         *
         * @return {@code true} if there were no failures
         */
        public boolean wasSuccessful() {
            return failures.isEmpty();
        }

        /**
         * Returns the failures collected during the campaign.
         *
         * @return the failures, empty on success
         */
        public List<Throwable> getFailures() {
            return failures;
        }
    }
}
