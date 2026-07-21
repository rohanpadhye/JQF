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

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.difffuzz.DiffFuzz;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

public class TrialRunner extends BlockJUnit4ClassRunner {
    private final FrameworkMethod method;
    protected final Object[] args;

    /** output of most recent trial */
    private Object output;

    public TrialRunner(Class<?> testClass, FrameworkMethod method, Object[] args) throws InitializationError {
        super(testClass);
        this.method = method;
        this.args = args;
    }

    @Override protected List<FrameworkMethod> computeTestMethods() {
        List<FrameworkMethod> diffMethods = getTestClass().getAnnotatedMethods(DiffFuzz.class);
        List<FrameworkMethod> fuzzMethods = getTestClass().getAnnotatedMethods(Fuzz.class);
        List<FrameworkMethod> testMethods = new ArrayList<>();
        if(diffMethods.size() > 0) testMethods.addAll(diffMethods);
        if(fuzzMethods.size() > 0) testMethods.addAll(fuzzMethods);
        return testMethods;
    }

    @Override protected Statement methodInvoker(
            FrameworkMethod frameworkMethod,
            Object test) {
        assert(this.method == frameworkMethod);
        return new Statement() {
            @Override public void evaluate() throws Throwable {
                output = frameworkMethod.invokeExplosively(test, args);
            }
        };
    }

    public void run() throws Throwable {
        this.methodBlock(method).evaluate();
    }

    public Object getOutput() {
        return output;
    }
}
