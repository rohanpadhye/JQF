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
package edu.berkeley.cs.jqf.fuzz.guidance;

import java.lang.reflect.Method;
import java.util.Random;

import edu.berkeley.cs.jqf.fuzz.FuzzResult;
import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGenerator;
import edu.berkeley.cs.jqf.junit5.JUnit5FuzzRunner;
import org.junit.Assert;
import org.junit.Test;

/**
 * Drives a real, coverage-instrumented Zest campaign through the JUnit 5 trial
 * executor and the framework-independent engine, and checks it matches the JUnit 4
 * path.
 *
 * <p>{@code SimpleClassTest.testWithGenerator} has no setup, so running it through
 * {@code edu.berkeley.cs.jqf.junit5.Junit5TrialExecutor} must explore exactly the
 * same branches as the JUnit 4 {@code TrialRunner} does in {@link ZestGuidanceIT}.
 * The argument generator is the same junit-quickcheck provider, discovered through
 * {@code ServiceLoader}.
 */
public class JUnit5ExecutorIT extends AbstractGuidanceIT {

    @Test
    public void testSimpleTestCoverageViaJUnit5Executor() throws Exception {
        String clazz = "edu.berkeley.cs.jqf.examples.simple.SimpleClassTest";
        String method = "testWithGenerator";
        long trials = 5000;
        Random rnd = new Random(42);

        Class<?> testClass = classLoader.loadClass(clazz);
        Method testMethod = testClass.getMethod(method, Integer.class);

        ZestGuidance zest = new ZestGuidance("SimpleClassTest", null, trials, resultsDir, rnd);
        ArgumentsGenerator generator = JUnit5FuzzRunner.resolveArguments(testClass, testMethod);

        FuzzResult result = JUnit5FuzzRunner.fuzz(testClass, testMethod, zest, generator);

        // The same seven branches as the JUnit 4 path in ZestGuidanceIT.
        Assert.assertEquals(7, zest.getTotalCoverage().getNonZeroCount());
        Assert.assertTrue("a clean target should report no failures", result.wasSuccessful());
    }
}
