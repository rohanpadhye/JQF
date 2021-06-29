/*
 * Copyright (c) 2021 Isabella Laybourn
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
package edu.berkeley.cs.jqf.fuzz.mutation;

import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.junit.TrialRunner;
import edu.berkeley.cs.jqf.instrument.InstrumentationException;
import edu.berkeley.cs.jqf.instrument.mutation.CartographyClassLoader;
import edu.berkeley.cs.jqf.instrument.mutation.MutationClassLoaders;
import edu.berkeley.cs.jqf.instrument.mutation.MutationInstance;
import edu.berkeley.cs.jqf.instrument.tracing.TraceLogger;
import edu.berkeley.cs.jqf.instrument.tracing.events.KillEvent;
import edu.berkeley.cs.jqf.instrument.util.ThrowingFunction;

import org.junit.AssumptionViolatedException;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Guidance that performs mutation-guided fuzzing
 *
 * @author Bella Laybourn
 */
public class MutationGuidance extends ZestGuidance {
    /** List of classes which should be mutated, and which shouldn't be mutated */
    private String[] mutables, immutables;

    /** The initial classLoader */
    private CartographyClassLoader cartographyClassLoader;

    /** The generated classloaders */
    private MutationClassLoaders mutationClassLoaders;
    
    /** The mutants killed so far */
    private Set<MutationInstance> deadMutants = new HashSet<>();

    /** The number of actual runs of the test */
    private long numRuns = 0;

    /** The number of runs done in the last interval */ 
    private long lastNumRuns = 0;

    public MutationGuidance(String testName, Duration duration, File outputDirectory, String include, String exclude)
            throws IOException {
        super(testName, duration, outputDirectory);
        if (include != null && !include.equals("")) {
            mutables = include.split(",");
        } else {
            mutables = new String[0];
        }
        if (exclude != null && !exclude.equals("")) {
            immutables = exclude.split(",");
        } else {
            immutables = new String[0];
        }
        totalCoverage = new MutationCoverage();
        runCoverage = new MutationCoverage();
        validCoverage = new MutationCoverage();
    }

    public MutationGuidance(String testName, Duration duration, File outputDirectory, File[] seedInputFiles, String include, String exclude) throws IOException {
        super(testName, duration, outputDirectory, seedInputFiles);
        if(include != null && !include.equals("")) {
            mutables = include.split(",");
        } else {
            mutables = new String[0];
        }
        if(exclude != null && !exclude.equals("")) {
            immutables = exclude.split(",");
        } else {
            immutables = new String[0];
        }
        totalCoverage = new MutationCoverage();
        runCoverage = new MutationCoverage();
        validCoverage = new MutationCoverage();
    }

    public MutationGuidance(String testName, Duration duration, File outputDirectory, File seedInputDir, String include, String exclude) throws IOException {
        super(testName, duration, outputDirectory, seedInputDir);
        if(include != null && !include.equals("")) {
            mutables = include.split(",");
        } else {
            mutables = new String[0];
        }
        if(exclude != null && !exclude.equals("")) {
            immutables = exclude.split(",");
        } else {
            immutables = new String[0];
        }
        totalCoverage = new MutationCoverage();
        runCoverage = new MutationCoverage();
        validCoverage = new MutationCoverage();
    }

    @Override
    public void handleResult(Result result, Throwable error) throws GuidanceException {
        // Stop timeout handling
        this.runStart = null;

        // Increment run count
        this.numTrials++;

        boolean valid = result == Result.SUCCESS;

        if (valid) {
            // Increment valid counter
            numValid++;
        }

        if (result == Result.SUCCESS || (result == Result.INVALID && SAVE_ONLY_VALID == false)) {

            int prevMutants = ((MutationCoverage) totalCoverage).numCaughtMutants();
            boolean mutantsUpdated = ((MutationCoverage) totalCoverage).updateMutants(((MutationCoverage) runCoverage));
            int newMutants = ((MutationCoverage) totalCoverage).numCaughtMutants();
            Set<Object> runMutants = ((MutationCoverage) runCoverage).getMutants();

            // Coverage before
            int nonZeroBefore = totalCoverage.getNonZeroCount();
            int validNonZeroBefore = validCoverage.getNonZeroCount();

            // Compute a list of keys for which this input can assume responsiblity.
            // Newly covered branches are always included.
            // Existing branches *may* be included, depending on the heuristics used.
            // A valid input will steal responsibility from invalid inputs
            Set<Object> responsibilities = computeResponsibilities(valid);

            // Update total coverage
            boolean coverageBitsUpdated = totalCoverage.updateBits(runCoverage);
            if (valid) {
                validCoverage.updateBits(runCoverage);
            }

            // Coverage after
            int nonZeroAfter = totalCoverage.getNonZeroCount();
            if (nonZeroAfter > maxCoverage) {
                maxCoverage = nonZeroAfter;
            }
            int validNonZeroAfter = validCoverage.getNonZeroCount();

            // Possibly save input
            boolean toSave = false;
            String why = "";

            if (mutantsUpdated) {
                toSave = true;
                why = why + "+mutants";
            }

            if (!DISABLE_SAVE_NEW_COUNTS && coverageBitsUpdated) {
                toSave = true;
                why = why + "+count";
            }

            // Save if new total coverage found
            if (nonZeroAfter > nonZeroBefore) {
                // Must be responsible for some branch
                assert(responsibilities.size() > 0);
                toSave = true;
                why = why + "+cov";
            }

            // Save if new valid coverage is found
            if (this.validityFuzzing && validNonZeroAfter > validNonZeroBefore) {
                // Must be responsible for some branch
                assert(responsibilities.size() > 0);
                currentInput.valid = true;
                toSave = true;
                why = why + "+valid";
            }

            if (toSave) {

                // Trim input (remove unused keys)
                currentInput.gc();

                // It must still be non-empty
                assert(currentInput.size() > 0) : String.format("Empty input: %s", currentInput.desc);

                // libFuzzerCompat stats are only displayed when they hit new coverage
                if (LIBFUZZER_COMPAT_OUTPUT) {
                    displayStats();
                }

                infoLog("Saving new input (at run %d): " +
                                "input #%d " +
                                "of size %d; " +
                                "total coverage = %d",
                        numTrials,
                        savedInputs.size(),
                        currentInput.size(),
                        nonZeroAfter);

                // Save input to queue and to disk
                final String reason = why;
                responsibilities.addAll(runMutants);
                GuidanceException.wrap(() -> saveCurrentInput(responsibilities, reason));

            }
        } else if (result == Result.FAILURE || result == Result.TIMEOUT) {
            String msg = error.getMessage();

            // Get the root cause of the failure
            Throwable rootCause = error;
            while (rootCause.getCause() != null) {
                rootCause = rootCause.getCause();
            }

            // Attempt to add this to the set of unique failures
            if (uniqueFailures.add(Arrays.asList(rootCause.getStackTrace()))) {

                // Trim input (remove unused keys)
                currentInput.gc();

                // It must still be non-empty
                assert(currentInput.size() > 0) : String.format("Empty input: %s", currentInput.desc);

                // Save crash to disk
                int crashIdx = uniqueFailures.size()-1;
                String saveFileName = String.format("id_%06d", crashIdx);
                File saveFile = new File(savedFailuresDirectory, saveFileName);
                GuidanceException.wrap(() -> writeCurrentInputToFile(saveFile));
                infoLog("%s","Found crash: " + error.getClass() + " - " + (msg != null ? msg : ""));
                String how = currentInput.desc;
                String why = result == Result.FAILURE ? "+crash" : "+hang";
                infoLog("Saved - %s %s %s", saveFile.getPath(), how, why);

                if (EXACT_CRASH_PATH != null && !EXACT_CRASH_PATH.equals("")) {
                    File exactCrashFile = new File(EXACT_CRASH_PATH);
                    GuidanceException.wrap(() -> writeCurrentInputToFile(exactCrashFile));
                }

                // libFuzzerCompat stats are only displayed when they hit new coverage or crashes
                if (LIBFUZZER_COMPAT_OUTPUT) {
                    displayStats();
                }

            }
        }

        // displaying stats on every interval is only enabled for AFL-like stats screen
        if (!LIBFUZZER_COMPAT_OUTPUT) {
            displayStats();
        }

        // Save input unconditionally if such a setting is enabled
        if (LOG_ALL_INPUTS) {
            File logDirectory = new File(allInputsDirectory, result.toString().toLowerCase());
            String saveFileName = String.format("id_%09d", numTrials);
            File saveFile = new File(logDirectory, saveFileName);
            GuidanceException.wrap(() -> writeCurrentInputToFile(saveFile));
        }
    }

    @Override
    protected void completeCycle() {
        cyclesCompleted++;
        infoLog("\n# Cycle " + cyclesCompleted + " completed.");

        // Go over all inputs and do a sanity check (plus log)
        infoLog("Here is a list of favored inputs:");
        int sumResponsibilities = 0;
        numFavoredLastCycle = 0;
        for (Input input : savedInputs) {
            if (input.isFavored()) {
                int responsibleFor = input.responsibilities.size();
                infoLog("Input %d is responsible for %d branches", input.id, responsibleFor);
                sumResponsibilities += responsibleFor;
                numFavoredLastCycle++;
            }
        }
        infoLog("\n\n\n");
    }

    @Override
    public ClassLoader getClassLoader(String[] classStrings, ClassLoader parent) throws MalformedURLException {
        if (this.cartographyClassLoader == null) {
            URL[] classPath =  Arrays.stream(classStrings).map(ThrowingFunction.wrap(x -> new File(x).toURI().toURL())).toArray(URL[]::new);
            this.cartographyClassLoader = new CartographyClassLoader(classPath, mutables, immutables, parent);
            this.mutationClassLoaders = new MutationClassLoaders(classPath, parent);
        }
        return this.cartographyClassLoader;
    }

    @Override
    public void run(TestClass testClass, FrameworkMethod method, Object[] args) throws Throwable {
        numRuns++;
        new TrialRunner(testClass.getJavaClass(), method, args).run(); // loaded by CartographyClassLoader
        List<Throwable> fails = new ArrayList<>();
        List<Class<?>> expectedExceptions = Arrays.asList(method.getMethod().getExceptionTypes());
        for (MutationInstance mutationInstance : cartographyClassLoader.getCartograph()) {
            if (!deadMutants.contains(mutationInstance)) {
                try {
                    mutationInstance.resetTimer();
                    Class<?> clazz = Class.forName(testClass.getName(), true, mutationClassLoaders.get(mutationInstance));
                    numRuns++;
                    new TrialRunner(clazz,
                            new FrameworkMethod(
                                    clazz.getMethod(method.getName(), method.getMethod().getParameterTypes())),
                            args).run();
                } catch (InstrumentationException e) {
                    throw new GuidanceException(e);
                } catch (GuidanceException e) {
                    throw e;
                } catch (AssumptionViolatedException e) {
                    // ignored
                } catch (Throwable e) {
                    if (!isExceptionExpected(e.getClass(), expectedExceptions)) {
                        // failed
                        deadMutants.add(mutationInstance);
                        TraceLogger.get().emit(new KillEvent(0, null, 0, mutationInstance)); // temp 0 values
                        fails.add(e);
                    }
                }
                // run
                ((MutationCoverage) runCoverage).see(mutationInstance);
            }
        }
    }

    private boolean isExceptionExpected(Class<? extends Throwable> e, List<Class<?>> expectedExceptions) {
        for (Class<?> expectedException : expectedExceptions) {
            if (expectedException.isAssignableFrom(e)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void displayStats() {
        Date now = new Date();
        long intervalTime = now.getTime() - lastRefreshTime.getTime();
        long totalTime = now.getTime() - startTime.getTime();

        if (intervalTime < STATS_REFRESH_TIME_PERIOD) {
            return;
        }

        double trialsPerSec = numTrials * 1000L / totalTime;
        long interlvalTrials = numTrials - lastNumTrials;
        double intervalTrialsPerSec = interlvalTrials * 1000.0 / intervalTime;

        double runsPerSec = numRuns * 1000L / totalTime;
        long intervalRuns = numRuns - lastNumRuns;
        double intervalRunsPerSec = intervalRuns * 1000.0 / intervalTime;

        lastRefreshTime = now;
        lastNumTrials = numTrials;
        lastNumRuns = numRuns;

        String currentParentInputDesc;
        if (seedInputs.size() > 0 || savedInputs.isEmpty()) {
            currentParentInputDesc = "<seed>";
        } else {
            Input currentParentInput = savedInputs.get(currentParentInputIdx);
            currentParentInputDesc = currentParentInputIdx + " ";
            currentParentInputDesc += currentParentInput.isFavored() ? "(favored)" : "(not favored)";
            currentParentInputDesc += " {" + numChildrenGeneratedForCurrentParentInput +
                    "/" + getTargetChildrenForParent(currentParentInput) + " mutations}";
        }

        int nonZeroCount = totalCoverage.getNonZeroCount();
        double nonZeroFraction = nonZeroCount * 100.0 / totalCoverage.size();
        int nonZeroValidCount = validCoverage.getNonZeroCount();
        double nonZeroValidFraction = nonZeroValidCount * 100.0 / validCoverage.size();

        if (console != null) {
            if (LIBFUZZER_COMPAT_OUTPUT) {
                console.printf("#%,d\tNEW\tcov: %,d exec/s: %,d L: %,d\n", numTrials, nonZeroValidCount, (long) intervalTrialsPerSec, currentInput.size());
            } else if (!QUIET_MODE) {
                console.printf("\033[2J");
                console.printf("\033[H");
                console.printf(this.getTitle() + "\n");
                if (this.testName != null) {
                    console.printf("Test name:            %s\n", this.testName);
                }
                console.printf("Results directory:    %s\n", this.outputDirectory.getAbsolutePath());
                console.printf("Elapsed time:         %s (%s)\n", millisToDuration(totalTime),
                        maxDurationMillis == Long.MAX_VALUE ? "no time limit" : ("max " + millisToDuration(maxDurationMillis)));
                console.printf("Number of trials:     %,d\n", numTrials);
                console.printf("Number of executions: %,d\n", numRuns);
                console.printf("Valid inputs:         %,d (%.2f%%)\n", numValid, numValid * 100.0 / numTrials);
                console.printf("Cycles completed:     %d\n", cyclesCompleted);
                console.printf("Unique failures:      %,d\n", uniqueFailures.size());
                console.printf("Queue size:           %,d (%,d favored last cycle)\n", savedInputs.size(), numFavoredLastCycle);
                console.printf("Current parent input: %s\n", currentParentInputDesc);
                console.printf("Fuzzing Throughput:   %,d/sec now | %,d/sec overall\n", (long) intervalTrialsPerSec, (long) trialsPerSec);
                console.printf("Execution Speed:      %,d/sec now | %,d/sec overall\n", (long) intervalRunsPerSec, (long) runsPerSec);
                console.printf("Total coverage:       %,d branches (%.2f%% of map)\n", nonZeroCount, nonZeroFraction);
                console.printf("Valid coverage:       %,d branches (%.2f%% of map)\n", nonZeroValidCount, nonZeroValidFraction);
                console.printf("Total coverage:       %,d mutants\n", ((MutationCoverage) totalCoverage).numCaughtMutants());
                console.printf("Available to Cover:   %,d mutants\n",((MutationCoverage) totalCoverage).numSeenMutants());
            }
        }

        String plotData = String.format("%d, %d, %d, %d, %d, %d, %.2f%%, %d, %d, %d, %.2f, %d, %d, %.2f%%",
                TimeUnit.MILLISECONDS.toSeconds(now.getTime()), cyclesCompleted, currentParentInputIdx,
                numSavedInputs, 0, 0, nonZeroFraction, uniqueFailures.size(), 0, 0, intervalTrialsPerSec,
                numValid, numTrials-numValid, nonZeroValidFraction);
        appendLineToFile(statsFile, plotData);

    }

    @Override
    protected String getTitle() {
        if (blind) {
            return  "Generator-based random fuzzing (no guidance)\n" +
                    "--------------------------------------------\n";
        } else {
            return  "Mutation-Guided Fuzzing\n" +
                    "--------------------------\n";
        }
    }
}
