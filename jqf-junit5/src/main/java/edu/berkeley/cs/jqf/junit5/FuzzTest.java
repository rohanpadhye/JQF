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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGeneratorFactory;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Marks a method as a JQF/Zest fuzz-test entry point for JUnit 5 (Jupiter).
 *
 * <p>A {@code @FuzzTest} method is a {@link TestTemplate} run by the standard
 * {@code junit-jupiter-engine} through {@link JQFTestExtension}. It has two modes,
 * selected at run time:
 *
 * <ul>
 *   <li><b>Regression</b> (the default for a plain {@code mvn test}): the method is
 *       replayed, once per saved input, against the corpus from a previous fuzzing
 *       run plus any {@link #seeds() seed} files plus one empty input. This is
 *       bounded and fast, and runs under the normal Jupiter lifecycle.</li>
 *   <li><b>Fuzzing</b> (when {@code -Djqf.fuzz=true} or the {@code JQF_FUZZ}
 *       environment variable is set, or a {@link #repro()} input is given): a full
 *       coverage-guided Zest campaign drives the method, and the first failing input
 *       fails the test.</li>
 * </ul>
 *
 * <p>The argument generator is not fixed: it is discovered through
 * {@link java.util.ServiceLoader} from the {@link ArgumentsGeneratorFactory}
 * providers on the test classpath (for example {@code jqf-generator-quickcheck}),
 * or chosen per test with {@link #arguments()}. If no provider is found, the test
 * fails fast with a message naming a provider to add.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@TestTemplate
@ExtendWith(JQFTestExtension.class)
public @interface FuzzTest {

    /**
     * The maximum wall-clock duration of a fuzzing campaign, for example
     * {@code "30s"}, {@code "10m"}, {@code "2h"} or {@code "1d"}; a bare number is
     * read as seconds. Empty means no time limit from the annotation. Overridden by
     * the {@code jqf.fuzz.duration} system property.
     *
     * @return the campaign duration specification, or an empty string
     */
    String maxDuration() default "";

    /**
     * The maximum number of trials in a fuzzing campaign. {@code 0} means no trial
     * limit from the annotation (the campaign is then bounded by duration).
     * Overridden by the {@code jqf.fuzz.trials} system property.
     *
     * @return the trial limit, or {@code 0} for the default
     */
    long maxTrials() default 0;

    /**
     * A path to an input file or directory to replay instead of fuzzing, mirroring
     * {@code @Fuzz(repro = ...)}. When set, the test runs in fuzzing mode against
     * exactly these inputs. Overridden by the {@code jqf.repro} system property.
     *
     * @return the replay path, or an empty string
     */
    String repro() default "";

    /**
     * A path to a file or directory of seed inputs used by the regression run, in
     * addition to the saved corpus and the empty input.
     *
     * @return the seed path, or an empty string
     */
    String seeds() default "";

    /**
     * The argument-generator provider for this test. The default,
     * {@link ArgumentsGeneratorFactory} itself, is a marker meaning "discover the
     * provider through {@link java.util.ServiceLoader}". Set it to a concrete
     * factory (with a public no-argument constructor) to choose one explicitly.
     *
     * @return the generator factory class, or the marker default
     */
    Class<? extends ArgumentsGeneratorFactory> arguments() default ArgumentsGeneratorFactory.class;
}
