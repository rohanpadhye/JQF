/*
 * Copyright (c) 2017-2018 The Regents of the University of California
 * Copyright (c) 2020-2021 Rohan Padhye
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
package edu.berkeley.cs.jqf.fuzz;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;

import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGenerator;
import edu.berkeley.cs.jqf.fuzz.spi.ResultClassifier;
import edu.berkeley.cs.jqf.fuzz.spi.SkipTrialException;
import edu.berkeley.cs.jqf.fuzz.spi.TrialExecutor;
import edu.berkeley.cs.jqf.fuzz.util.Observability;
import edu.berkeley.cs.jqf.instrument.InstrumentationException;

import static edu.berkeley.cs.jqf.fuzz.guidance.Result.FAILURE;
import static edu.berkeley.cs.jqf.fuzz.guidance.Result.INVALID;
import static edu.berkeley.cs.jqf.fuzz.guidance.Result.SUCCESS;
import static edu.berkeley.cs.jqf.fuzz.guidance.Result.TIMEOUT;

/**
 * The fuzzing loop, independent of any test framework.
 *
 * <p>For each input offered by the {@link Guidance}, the loop generates arguments
 * through an {@link ArgumentsGenerator}, runs one trial through a
 * {@link TrialExecutor}, classifies the outcome with a {@link ResultClassifier},
 * and reports it back to the guidance. None of these collaborators expose a
 * test-framework type, so the loop carries no dependency on JUnit or any
 * generator library.
 *
 * <p>When the campaign ends, the loop rethrows a single collected failure as-is,
 * or wraps several in a {@link MultipleFailuresError}.
 */
public final class FuzzRunner {

    private final ArgumentsGenerator argumentsGenerator;
    private final TrialExecutor trialExecutor;
    private final Guidance guidance;
    private final ResultClassifier classifier;
    private final List<Class<?>> expectedExceptions;
    private final Observability observability;
    private final boolean skipExceptionSwallow;

    /**
     * Creates a runner for one test method.
     *
     * @param argumentsGenerator generates each trial's arguments
     * @param trialExecutor      runs a single trial with proper lifecycle
     * @param guidance           drives input selection and observes results
     * @param classifier         maps trial throwables onto engine outcomes
     * @param expectedExceptions exception types declared by the test method;
     *                           a thrown subtype counts as success
     * @param observability      collector for optional observability logging
     * @param skipExceptionSwallow if {@code true}, declared exceptions count as
     *                           failures rather than success
     */
    public FuzzRunner(ArgumentsGenerator argumentsGenerator,
                      TrialExecutor trialExecutor,
                      Guidance guidance,
                      ResultClassifier classifier,
                      List<Class<?>> expectedExceptions,
                      Observability observability,
                      boolean skipExceptionSwallow) {
        this.argumentsGenerator = argumentsGenerator;
        this.trialExecutor = trialExecutor;
        this.guidance = guidance;
        this.classifier = classifier;
        this.expectedExceptions = expectedExceptions;
        this.observability = observability;
        this.skipExceptionSwallow = skipExceptionSwallow;
    }

    /**
     * Runs the fuzzing loop until the guidance stops offering input.
     *
     * @throws Throwable a single trial failure, a {@link MultipleFailuresError}
     *                   for several, or a {@link GuidanceException} if fuzzing was
     *                   interrupted
     */
    public void run() throws Throwable {
        List<Throwable> failures = new ArrayList<>();
        boolean observe = System.getProperty("jqfObservability") != null;

        try {
            while (guidance.hasInput()) {
                Result result = INVALID;
                Throwable error = null;
                long startTrialTime = System.currentTimeMillis();
                long endGenerationTime = startTrialTime;
                Object[] args = {};

                try {
                    // Generate input values from a file-backed source of randomness
                    try {
                        StreamBackedRandom randomFile =
                                new StreamBackedRandom(guidance.getInput(), Long.BYTES);
                        args = argumentsGenerator.generate(randomFile);
                        guidance.observeGeneratedArgs(args);
                    } catch (SkipTrialException e) {
                        // A generator gave up on this input; try the next one
                        continue;
                    } catch (GuidanceException e) {
                        throw e;
                    } catch (IllegalStateException e) {
                        if (e.getCause() instanceof EOFException) {
                            // Ran out of input before all arguments were built; try the next one
                            continue;
                        }
                        // Preserve legacy behaviour: a non-EOF illegal state surfaces as a trial outcome
                        throw e;
                    } catch (Throwable e) {
                        if (classifier.isAssumptionViolation(e) || classifier.isTimeout(e)) {
                            // The generator gave up on this input; try the next one
                            continue;
                        }
                        throw new GuidanceException(e);
                    }

                    endGenerationTime = System.currentTimeMillis();

                    // Run the trial
                    guidance.beforeRun();
                    trialExecutor.runTrial(args);

                    // If we reached here, the trial was a success
                    result = SUCCESS;
                } catch (InstrumentationException e) {
                    throw new GuidanceException(e);
                } catch (GuidanceException e) {
                    throw e;
                } catch (Throwable e) {
                    if (classifier.isAssumptionViolation(e)) {
                        result = INVALID;
                        error = e;
                    } else if (classifier.isTimeout(e)) {
                        result = TIMEOUT;
                        error = e;
                    } else if (isExceptionExpected(e.getClass())) {
                        result = SUCCESS; // Swallow the declared exception
                    } else {
                        result = FAILURE;
                        error = e;
                        failures.add(e);
                    }
                }
                long endTrialTime = System.currentTimeMillis();

                if (observe) {
                    observability.addStatus(result);
                    if (result == SUCCESS) {
                        observability.addTiming(startTrialTime, endGenerationTime, endTrialTime);
                    }
                    observability.addArgs(args);
                    observability.add("how_generated", guidance.observeGuidance());
                    observability.writeToFile();
                }

                // Inform the guidance about the outcome of this trial
                try {
                    guidance.handleResult(result, error);
                } catch (GuidanceException e) {
                    throw e;
                } catch (Throwable e) {
                    throw new GuidanceException(e);
                }
            }
        } catch (GuidanceException e) {
            System.err.println("Fuzzing stopped due to guidance exception: " + e.getMessage());
            throw e;
        }

        if (!failures.isEmpty()) {
            if (failures.size() == 1) {
                throw failures.get(0);
            } else {
                throw new MultipleFailuresError(failures);
            }
        }
    }

    private boolean isExceptionExpected(Class<? extends Throwable> e) {
        if (skipExceptionSwallow) {
            return false;
        }
        for (Class<?> expectedException : expectedExceptions) {
            if (expectedException.isAssignableFrom(e)) {
                return true;
            }
        }
        return false;
    }
}
