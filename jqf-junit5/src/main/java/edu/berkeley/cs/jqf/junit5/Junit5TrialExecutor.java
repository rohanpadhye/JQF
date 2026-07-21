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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import edu.berkeley.cs.jqf.fuzz.spi.TrialExecutor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Runs trials reflectively under a JUnit 5 (Jupiter) lifecycle.
 *
 * <p>The fuzzing loop runs millions of trials, so it must not re-enter the
 * Jupiter engine per trial. This executor instead replays the relevant
 * lifecycle directly: {@code @BeforeAll}/{@code @AfterAll} run once around the
 * whole campaign, and every trial gets a fresh test instance with its
 * {@code @BeforeEach}/{@code @AfterEach} callbacks. The test method's return
 * value is captured for differential fuzzing.
 *
 * <p>This is a pragmatic subset of Jupiter's lifecycle: it honours inheritance
 * (superclass callbacks run first for "before", last for "after") but does not
 * resolve parameters into lifecycle methods, nor support {@code @Nested} classes
 * or {@code PER_CLASS} instances. The regression run path uses the real Jupiter
 * engine instead, so this executor only drives the dedicated fuzzing campaign.
 */
public final class Junit5TrialExecutor implements TrialExecutor {

    private final Class<?> testClass;
    private final Constructor<?> constructor;
    private final Method testMethod;
    private final List<Method> beforeAllMethods;
    private final List<Method> afterAllMethods;
    private final List<Method> beforeEachMethods;
    private final List<Method> afterEachMethods;

    private boolean beforeAllExecuted = false;
    private Object lastOutput;

    public Junit5TrialExecutor(Class<?> testClass, Method testMethod) {
        this.testClass = testClass;
        this.testMethod = testMethod;
        this.testMethod.setAccessible(true);
        try {
            this.constructor = testClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("Test class " + testClass.getName()
                    + " must have a no-argument constructor to be fuzzed with @FuzzTest", e);
        }
        this.constructor.setAccessible(true);
        this.beforeAllMethods = lifecycleMethods(testClass, BeforeAll.class, true);
        this.afterAllMethods = lifecycleMethods(testClass, AfterAll.class, false);
        this.beforeEachMethods = lifecycleMethods(testClass, BeforeEach.class, true);
        this.afterEachMethods = lifecycleMethods(testClass, AfterEach.class, false);
    }

    @Override
    public void runTrial(Object[] args) throws Throwable {
        if (!beforeAllExecuted) {
            for (Method m : beforeAllMethods) {
                invoke(null, m);
            }
            beforeAllExecuted = true;
        }

        Object instance = constructor.newInstance();
        Throwable primary = null;
        try {
            for (Method m : beforeEachMethods) {
                invoke(instance, m);
            }
            lastOutput = invoke(instance, testMethod, args);
        } catch (Throwable t) {
            primary = t;
        } finally {
            for (Method m : afterEachMethods) {
                try {
                    invoke(instance, m);
                } catch (Throwable t) {
                    if (primary == null) {
                        primary = t;
                    } else {
                        primary.addSuppressed(t);
                    }
                }
            }
        }
        if (primary != null) {
            throw primary;
        }
    }

    @Override
    public Object getLastOutput() {
        return lastOutput;
    }

    /**
     * Runs the {@code @AfterAll} callbacks once, after the campaign has finished.
     *
     * <p>This is outside the {@link TrialExecutor} contract because the engine
     * has no end-of-campaign hook; the campaign driver calls it explicitly. It is
     * a no-op if no trial ever ran (so {@code @BeforeAll} never ran either).
     */
    public void runAfterAll() {
        if (!beforeAllExecuted) {
            return;
        }
        for (Method m : afterAllMethods) {
            try {
                invoke(null, m);
            } catch (Throwable t) {
                // Best effort: a campaign has already completed, so swallow teardown errors.
            }
        }
    }

    private static Object invoke(Object target, Method method, Object... args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause() != null ? e.getCause() : e;
        }
    }

    /**
     * Collects the methods annotated with {@code annotation} across the class
     * hierarchy, ordered superclass-first for "before" callbacks and
     * subclass-first for "after" callbacks.
     */
    private static List<Method> lifecycleMethods(Class<?> type, Class<? extends Annotation> annotation,
                                                 boolean superclassFirst) {
        Deque<Class<?>> hierarchy = new ArrayDeque<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            hierarchy.addFirst(c); // build superclass-first
        }
        List<Method> methods = new ArrayList<>();
        for (Class<?> c : hierarchy) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.isAnnotationPresent(annotation)) {
                    m.setAccessible(true);
                    methods.add(m);
                }
            }
        }
        if (!superclassFirst) {
            Collections.reverse(methods);
        }
        return methods;
    }
}
