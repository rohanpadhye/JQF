/*
 * Copyright (c) 2017-2018 The Regents of the University of California
 * Copyright (c) 2020-2021 Rohan Padhye
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
package edu.berkeley.cs.jqf.fuzz.junit.quickcheck;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import com.pholser.junit.quickcheck.internal.generator.GeneratorRepository;
import edu.berkeley.cs.jqf.fuzz.FuzzRunner;
import edu.berkeley.cs.jqf.fuzz.difffuzz.DiffFuzzGuidance;
import edu.berkeley.cs.jqf.fuzz.difffuzz.DiffTrialExecutor;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.junit.Junit4ResultClassifier;
import edu.berkeley.cs.jqf.fuzz.junit.Junit4TrialExecutor;
import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGenerator;
import edu.berkeley.cs.jqf.fuzz.spi.TrialExecutor;
import edu.berkeley.cs.jqf.fuzz.util.Observability;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

/**
 * A JUnit {@link Statement} that runs a method under guided fuzz testing.
 *
 * <p>This is the JUnit 4 bridge: it wires junit-quickcheck argument generation and
 * a JUnit 4 trial executor into the framework-independent {@link FuzzRunner}.
 *
 * @author Rohan Padhye
 */
public class FuzzStatement extends Statement {
    private final FrameworkMethod method;
    private final TestClass testClass;
    private final GeneratorRepository generatorRepository;
    private final Guidance guidance;

    public FuzzStatement(FrameworkMethod method, TestClass testClass,
                         GeneratorRepository generatorRepository, Guidance fuzzGuidance) {
        this.method = method;
        this.testClass = testClass;
        this.generatorRepository = generatorRepository;
        this.guidance = fuzzGuidance;
    }

    /**
     * Runs the fuzzing loop for this method.
     *
     * @throws Throwable if the test fails
     */
    @Override
    public void evaluate() throws Throwable {
        Class<?> javaClass = testClass.getJavaClass();
        Method javaMethod = method.getMethod();

        // Resolve junit-quickcheck generators for this method's parameters
        ArgumentsGenerator argumentsGenerator =
                new QuickcheckArgumentsGeneratorFactory(generatorRepository).create(javaClass, javaMethod);

        // Run each trial under JUnit 4, capturing the return value for differential fuzzing
        TrialExecutor trialExecutor = new Junit4TrialExecutor(javaClass, javaMethod);
        if (guidance instanceof DiffFuzzGuidance) {
            trialExecutor = new DiffTrialExecutor(trialExecutor, (DiffFuzzGuidance) guidance);
        }

        List<Class<?>> expectedExceptions = Arrays.asList(javaMethod.getExceptionTypes());
        boolean skipExceptionSwallow = Boolean.getBoolean("jqf.failOnDeclaredExceptions");
        Observability observability =
                new Observability(testClass.getName(), method.getName(), System.currentTimeMillis());

        new FuzzRunner(argumentsGenerator, trialExecutor, guidance, new Junit4ResultClassifier(),
                expectedExceptions, observability, skipExceptionSwallow).run();
    }
}
