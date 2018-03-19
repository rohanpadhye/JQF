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
package edu.berkeley.cs.jqf.fuzz;

import java.util.List;
import java.util.Random;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.internal.generator.GeneratorRepository;
import com.pholser.junit.quickcheck.internal.generator.ServiceLoaderGeneratorSource;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FuzzStatement;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * This class extends JUnit and Quickcheck runners to enable guided
 * fuzz testing. 
 *
 * @author Rohan Padhye
 */
public class JQF extends JUnitQuickcheck {

    protected final GeneratorRepository generatorRepository;

    @SuppressWarnings("unused") // Invoked reflectively by JUnit
    public JQF(Class<?> clazz) throws InitializationError {
        super(clazz);
        // Initialize generator repository with a deterministic seed (for reproducibility)
        SourceOfRandomness randomness = new SourceOfRandomness(new Random(42));
        this.generatorRepository = new GeneratorRepository(randomness).register(new ServiceLoaderGeneratorSource());;

    }

    @Override protected List<FrameworkMethod> computeTestMethods() {
        List<FrameworkMethod> methods = super.computeTestMethods();
        methods.addAll(getTestClass().getAnnotatedMethods(Fuzz.class));
        return methods;
    }


    @Override protected void validateTestMethods(List<Throwable> errors) {
        super.validateTestMethods(errors);
        validateFuzzMethods(errors);
    }

    private void validateFuzzMethods(List<Throwable> errors) {
        for (FrameworkMethod method : getTestClass().getAnnotatedMethods(Fuzz.class)) {
            method.validatePublicVoid(false, errors);
            if (method.getAnnotation(Property.class) != null) {
                errors.add(new Exception("Method " + method.getName() +
                        " cannot have both @Property and @Fuzz annotations"));
            }
        }
    }

    @Override public Statement methodBlock(FrameworkMethod method) {
        if (method.getAnnotation(Fuzz.class) != null) {
            return new FuzzStatement(method, getTestClass(), generatorRepository);
        }
        return super.methodBlock(method);
    }
}
