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
package edu.berkeley.cs.jqf.fuzz;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.internal.generator.GeneratorRepository;
import com.pholser.junit.quickcheck.internal.generator.ServiceLoaderGeneratorSource;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FuzzStatement;
import edu.berkeley.cs.jqf.fuzz.random.NoGuidance;
import edu.berkeley.cs.jqf.fuzz.repro.ReproGuidance;
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
        this.generatorRepository = new GeneratorRepository(randomness).register(new ServiceLoaderGeneratorSource());
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
        // JQF only needs special handling for @Fuzz-annotated test methods
        if (method.getAnnotation(Fuzz.class) == null) {
            return super.methodBlock(method);
        }

        // Get currently set fuzzing guidance
        Guidance guidance = GuidedFuzzing.getCurrentGuidance();

        // If nothing is set, default to random or repro
        if (guidance == null) {
            // Check for @Fuzz(repro=)
            String repro = method.getAnnotation(Fuzz.class).repro();
            if (repro.isEmpty()) {
                long maxTrials = Long.getLong("jqf.quickcheck.trials", GuidedFuzzing.DEFAULT_MAX_TRIALS);
                guidance = new NoGuidance(maxTrials, System.err);
            } else {
                String reproPath;
                // Check if repro path is variable (e.g. `${foo}`)
                if (repro.matches("\\$\\{[a-zA-Z.\\d_$]*\\}")) {
                    // Get a system property with that name (e.g. `foo`)
                    String key = repro.substring(2, repro.length()-1);
                    String val = System.getProperty(key);

                    // Check if such a property is set
                    if (val == null) {
                        throw new IllegalArgumentException(String.format("Test method has " +
                                        "@Fuzz annotation with repro=%s, but such a system " +
                                        "property is not set. Use `-D%s=<path>` when running.",
                                repro, key));
                    }

                    reproPath = val;
                } else {
                    // If it is not a variable, then treat it literally
                    reproPath = repro;
                }

                // Create a ReproGuidance with the given path
                File inputFile = new File(reproPath);
                try {
                    guidance = new ReproGuidance(inputFile, null);
                } catch (IOException e) {
                    throw new GuidanceException(String.format("Could not open repro file: %s",
                            inputFile.getAbsolutePath()), e);
                }
            }
        }

        return new FuzzStatement(method, getTestClass(), generatorRepository, guidance);
    }
}
