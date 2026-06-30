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
package edu.berkeley.cs.jqf.junit5;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import edu.berkeley.cs.jqf.fuzz.FuzzResult;
import edu.berkeley.cs.jqf.fuzz.FuzzRunner;
import edu.berkeley.cs.jqf.fuzz.MultipleFailuresError;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGenerator;
import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGeneratorFactory;
import edu.berkeley.cs.jqf.fuzz.util.Observability;
import edu.berkeley.cs.jqf.instrument.tracing.SingleSnoop;
import edu.berkeley.cs.jqf.instrument.tracing.TraceLogger;

/**
 * The reusable JUnit 5 fuzzing entry point.
 *
 * <p>It wires the framework-independent {@link FuzzRunner} to the JUnit 5
 * adapters: a {@link Junit5TrialExecutor} for trial execution and a
 * {@link Junit5ResultClassifier} for outcome classification. The argument
 * generator is resolved separately (so it stays pluggable). Both the Jupiter
 * extension and integration tests use this class.
 */
public final class JUnit5FuzzRunner {

    private JUnit5FuzzRunner() {
    }

    /**
     * Resolves the argument generator for a fuzz-test method.
     *
     * <p>If the method's {@link FuzzTest#arguments()} names a concrete factory,
     * that factory is used; otherwise the provider is discovered through
     * {@link ServiceLoader}. Methods without a {@code @FuzzTest} annotation (for
     * example a JUnit 4 {@code @Fuzz} method reused by an integration test) always
     * fall back to {@link ServiceLoader}.
     *
     * @param testClass  the test class
     * @param testMethod the fuzz-test method
     * @return a generator bound to the method
     * @throws IllegalStateException if no provider is available
     */
    public static ArgumentsGenerator resolveArguments(Class<?> testClass, Method testMethod) {
        Class<? extends ArgumentsGeneratorFactory> override = null;
        FuzzTest annotation = testMethod.getAnnotation(FuzzTest.class);
        if (annotation != null) {
            override = annotation.arguments();
        }
        ArgumentsGeneratorFactory factory =
                resolveFactory(override, ServiceLoader.load(ArgumentsGeneratorFactory.class));
        return factory.create(testClass, testMethod);
    }

    /**
     * Chooses an argument-generator factory, failing fast with a clear message when
     * none is available.
     *
     * <p>Package-private and taking the candidate providers as an argument so the
     * fail-fast behaviour can be unit-tested without depending on the classpath.
     *
     * @param override the explicitly requested factory class, the
     *                 {@link ArgumentsGeneratorFactory} marker for "use ServiceLoader",
     *                 or {@code null}
     * @param services the providers discovered on the classpath
     * @return the chosen factory
     * @throws IllegalStateException if {@code override} is the marker (or null) and
     *                               {@code services} is empty
     */
    static ArgumentsGeneratorFactory resolveFactory(Class<? extends ArgumentsGeneratorFactory> override,
                                                    Iterable<ArgumentsGeneratorFactory> services) {
        if (override != null && override != ArgumentsGeneratorFactory.class) {
            try {
                return override.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Could not instantiate the @FuzzTest(arguments) factory "
                        + override.getName() + "; it must have a public no-argument constructor.", e);
            }
        }
        Iterator<ArgumentsGeneratorFactory> iterator = services.iterator();
        if (iterator.hasNext()) {
            return iterator.next();
        }
        throw new IllegalStateException(
                "No argument-generator provider found on the test classpath. Add one "
                        + "(for example edu.berkeley.cs.jqf:jqf-generator-quickcheck) so @FuzzTest methods "
                        + "can generate arguments, or set @FuzzTest(arguments = ...) to choose a provider "
                        + "explicitly.");
    }

    /**
     * Decodes one input into the arguments for a single trial.
     *
     * @param generator the argument generator
     * @param input     the raw bytes of one saved or seed input
     * @return the decoded arguments
     */
    public static Object[] decode(ArgumentsGenerator generator, InputStream input) {
        return generator.generate(input);
    }

    /**
     * Runs a full fuzzing campaign for a fuzz-test method under the given guidance.
     *
     * <p>The guidance's instrumentation callback is registered, and any tracer left
     * over from a previous campaign in this JVM is dropped, before snooping starts,
     * mirroring {@link FuzzRunner#run(Class, String, Guidance)}.
     *
     * @param testClass the test class
     * @param method    the fuzz-test method
     * @param guidance  the fuzzing guidance
     * @param arguments the argument generator
     * @return the campaign result
     */
    public static FuzzResult fuzz(Class<?> testClass, Method method, Guidance guidance,
                                  ArgumentsGenerator arguments) {
        Junit5TrialExecutor executor = new Junit5TrialExecutor(testClass, method);

        SingleSnoop.setCallbackGenerator(guidance::generateCallBack);
        TraceLogger.get().remove();

        List<Class<?>> expectedExceptions = Arrays.asList(method.getExceptionTypes());
        boolean skipExceptionSwallow = Boolean.getBoolean("jqf.failOnDeclaredExceptions");
        Observability observability =
                new Observability(testClass.getName(), method.getName(), System.currentTimeMillis());

        List<Throwable> failures = new ArrayList<>();
        try {
            SingleSnoop.startSnooping(testClass.getName() + "#" + method.getName());
            new FuzzRunner(arguments, executor, guidance, new Junit5ResultClassifier(),
                    expectedExceptions, observability, skipExceptionSwallow).run();
        } catch (MultipleFailuresError e) {
            failures.addAll(e.getFailures());
        } catch (Throwable e) {
            failures.add(e);
        } finally {
            executor.runAfterAll();
            TraceLogger.get().remove();
        }
        return new FuzzResult(failures);
    }
}
