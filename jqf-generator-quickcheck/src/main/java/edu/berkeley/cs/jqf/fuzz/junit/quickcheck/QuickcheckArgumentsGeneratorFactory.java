/*
 * Copyright (c) 2017-2018 The Regents of the University of California
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
package edu.berkeley.cs.jqf.fuzz.junit.quickcheck;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.internal.ParameterTypeContext;
import com.pholser.junit.quickcheck.internal.generator.GeneratorRepository;
import com.pholser.junit.quickcheck.internal.generator.ServiceLoaderGeneratorSource;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGenerator;
import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGeneratorFactory;
import ru.vyarus.java.generics.resolver.GenericsResolver;
import ru.vyarus.java.generics.resolver.context.MethodGenericsContext;

/**
 * Builds junit-quickcheck-backed {@link ArgumentsGenerator}s.
 *
 * <p>This is the reference generation provider. It resolves one
 * {@link Generator} per parameter through a {@link GeneratorRepository}, honouring
 * junit-quickcheck annotations such as {@code @From}, {@code @Size}, and
 * {@code @InRange}. The repository uses a fixed seed so generator setup is
 * reproducible; the per-trial randomness comes from the guided byte stream.
 *
 * <p>This class depends only on junit-quickcheck's generator API, not on its
 * JUnit 4 runner, so it can serve both the JUnit 4 and JUnit 5 run paths.
 */
public final class QuickcheckArgumentsGeneratorFactory implements ArgumentsGeneratorFactory {

    private final GeneratorRepository repository;

    /**
     * Creates a factory with its own generator repository, seeded for reproducible setup.
     */
    public QuickcheckArgumentsGeneratorFactory() {
        this(defaultRepository());
    }

    /**
     * Creates a factory that resolves generators from the given repository.
     *
     * @param repository the generator repository to resolve parameters against
     */
    public QuickcheckArgumentsGeneratorFactory(GeneratorRepository repository) {
        this.repository = repository;
    }

    private static GeneratorRepository defaultRepository() {
        SourceOfRandomness randomness = new SourceOfRandomness(new Random(42));
        return new GeneratorRepository(randomness).register(new ServiceLoaderGeneratorSource());
    }

    @Override
    public ArgumentsGenerator create(Class<?> testClass, Method testMethod) {
        MethodGenericsContext generics = GenericsResolver.resolve(testClass).method(testMethod);
        List<Generator<?>> generators = Arrays.stream(testMethod.getParameters())
                .map(parameter -> ParameterTypeContext.forParameter(parameter, generics).annotate(parameter))
                .map(repository::produceGenerator)
                .collect(Collectors.toList());
        return new QuickcheckArgumentsGenerator(generators);
    }
}
