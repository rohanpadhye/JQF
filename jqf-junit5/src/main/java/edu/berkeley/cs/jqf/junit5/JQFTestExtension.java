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
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import edu.berkeley.cs.jqf.fuzz.FuzzResult;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGenerator;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

/**
 * The Jupiter extension behind {@link FuzzTest}.
 *
 * <p>It is a {@link TestTemplateInvocationContextProvider}: it turns one
 * {@code @FuzzTest} method into either a bounded set of regression invocations or a
 * single fuzzing invocation, depending on {@link JUnit5FuzzConfig#isFuzzingMode()}.
 *
 * <ul>
 *   <li><b>Regression</b>: one invocation per input (saved corpus, seeds, and one
 *       empty input). Each invocation injects its decoded arguments through a
 *       {@link GeneratedArgumentsResolver} and runs the real test method under the
 *       Jupiter lifecycle via {@code invocation.proceed()}.</li>
 *   <li><b>Fuzzing</b>: a single invocation whose method body is skipped; the whole
 *       Zest campaign is driven through {@link JUnit5FuzzRunner}, and the first
 *       failing input is rethrown as a {@link JqfFinding} so the node fails.</li>
 * </ul>
 */
public final class JQFTestExtension
        implements TestTemplateInvocationContextProvider, InvocationInterceptor, ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return context.getTestMethod()
                .filter(m -> m.isAnnotationPresent(FuzzTest.class))
                .map(m -> ConditionEvaluationResult.enabled("@FuzzTest is always enabled; its mode is chosen at run time"))
                .orElseGet(() -> ConditionEvaluationResult.enabled("Not a @FuzzTest method"));
    }

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return context.getTestMethod().map(m -> m.isAnnotationPresent(FuzzTest.class)).orElse(false);
    }

    @Override
    public boolean mayReturnZeroTestTemplateInvocationContexts(ExtensionContext context) {
        // A regression run whose only input (the all-zero seed) cannot generate the arguments
        // produces no invocation. That is a skipped node, not a configuration error.
        return true;
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        Method method = context.getRequiredTestMethod();
        JUnit5FuzzConfig config = JUnit5FuzzConfig.from(testClass, method);

        if (config.isFuzzingMode()) {
            // One invocation: the campaign is driven in interceptTestTemplateMethod.
            // Provide placeholder arguments only so Jupiter can resolve the (skipped) call.
            return Stream.of(new NamedInvocationContext("fuzzing", defaultArguments(method)));
        }

        // Regression: replay each input through the real test method.
        ArgumentsGenerator generator = JUnit5FuzzRunner.resolveArguments(testClass, method);
        List<TestTemplateInvocationContext> contexts = new ArrayList<>();
        for (JUnit5FuzzConfig.NamedInput input : config.regressionInputs()) {
            Object[] arguments;
            try (InputStream stream = input.open()) {
                arguments = JUnit5FuzzRunner.decode(generator, stream);
            } catch (Exception e) {
                // An input that cannot produce arguments (for example a truncated file) is skipped.
                continue;
            }
            contexts.add(new NamedInvocationContext(input.name(), arguments));
        }
        if (contexts.isEmpty()) {
            Object[] fallback = defaultArguments(method);
            // With no corpus, replay the all-zero input. Type-default primitives make a valid
            // trial, but a null object default would only NPE the test body, so leave the node with
            // no invocation (reported as skipped) rather than running it with a null.
            if (!containsNull(fallback)) {
                contexts.add(new NamedInvocationContext("zeros", fallback));
            }
        }
        return contexts.stream();
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
                                            ReflectiveInvocationContext<Method> invocationContext,
                                            ExtensionContext extensionContext) throws Throwable {
        Class<?> testClass = extensionContext.getRequiredTestClass();
        Method method = extensionContext.getRequiredTestMethod();
        JUnit5FuzzConfig config = JUnit5FuzzConfig.from(testClass, method);

        if (!config.isFuzzingMode()) {
            // Regression: run the real test with the arguments injected by the resolver.
            invocation.proceed();
            return;
        }

        // Fuzzing: don't run the templated body; drive the campaign ourselves.
        invocation.skip();

        ArgumentsGenerator generator = JUnit5FuzzRunner.resolveArguments(testClass, method);
        boolean usingInstalledGuidance = JUnit5FuzzConfig.installedGuidance() != null;
        Guidance guidance = config.createGuidance();

        FuzzResult result;
        try {
            result = JUnit5FuzzRunner.fuzz(testClass, method, guidance, generator);
        } finally {
            if (usingInstalledGuidance) {
                JUnit5FuzzConfig.clearInstalledGuidance();
            }
        }

        if (!result.wasSuccessful()) {
            throw new JqfFinding(result.getFailures().get(0));
        }
    }

    private static boolean containsNull(Object[] arguments) {
        for (Object argument : arguments) {
            if (argument == null) {
                return true;
            }
        }
        return false;
    }

    private static Object[] defaultArguments(Method method) {
        Class<?>[] types = method.getParameterTypes();
        Object[] arguments = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            arguments[i] = defaultValue(types[i]);
        }
        return arguments;
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return Boolean.FALSE;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == double.class) {
            return 0d;
        }
        return null;
    }

    /**
     * A {@link TestTemplateInvocationContext} that supplies one invocation's
     * arguments through a {@link GeneratedArgumentsResolver}.
     */
    private static final class NamedInvocationContext implements TestTemplateInvocationContext {

        private final String name;
        private final Object[] arguments;

        NamedInvocationContext(String name, Object[] arguments) {
            this.name = name;
            this.arguments = arguments;
        }

        @Override
        public String getDisplayName(int invocationIndex) {
            return "[" + name + "]";
        }

        @Override
        public List<Extension> getAdditionalExtensions() {
            return Collections.singletonList(new GeneratedArgumentsResolver(arguments));
        }
    }
}
