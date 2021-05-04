package edu.berkeley.cs.jqf.fuzz.mutation;

import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.guidance.TimeoutException;
import edu.berkeley.cs.jqf.fuzz.junit.TrialRunner;
import edu.berkeley.cs.jqf.instrument.InstrumentationException;
import edu.berkeley.cs.jqf.instrument.tracing.TraceLogger;
import edu.berkeley.cs.jqf.instrument.tracing.events.KillEvent;
import mutation.CartographyClassLoader;
import mutation.MutationInstance;
import org.junit.AssumptionViolatedException;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MutationGuidance extends ZestGuidance {
    String[] mutables, immutables;
    CartographyClassLoader cartographyClassLoader;

    public MutationGuidance(String testName, Duration duration, File outputDirectory, String include, String exclude) throws IOException {
        super(testName, duration, outputDirectory);
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
            /*if (this.validityFuzzing && validNonZeroAfter > validNonZeroBefore) {
                // Must be responsible for some branch
                assert(responsibilities.size() > 0);
                currentInput.valid = true;
                toSave = true;
                why = why + "+valid";
            }*/

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
    public ClassLoader getClassLoader(String[] classPath, ClassLoader parent) throws MalformedURLException {
        cartographyClassLoader = new CartographyClassLoader(classPath, mutables, immutables, parent);
        return cartographyClassLoader;
    }

    @Override
    public void run(TestClass testClass, FrameworkMethod method, Object[] args) throws Throwable {
        //System.out.println("running " + testClass.getName());
        // BufferedWriter writer = null; //TODO
        new TrialRunner(testClass.getJavaClass(), method, args).run(); //loaded by CartographyClassLoader
        //System.out.println("loaded " + testClass.getName());
        long totalRun = 0, totalFail = 0, totalIgnore = 0;
        List<Throwable> fails = new ArrayList<>();
        List<Class<?>> expectedExceptions = Arrays.asList(method.getMethod().getExceptionTypes());
        for(MutationInstance mcl : cartographyClassLoader.getCartograph()) {
            if(!mcl.isDead()) {
                try {
                    //System.out.println("testClass: " + testClass.getName() + ",  method: " + method.getName());
                    //System.out.println("running mutant " + mcl);
                    Class<?> clazz = Class.forName(testClass.getName(), true, mcl);
                    //TODO don't want to hardcode args
                    //TODO make cartographyclassloader extend instrumentingclassloader --> code coverage + mutants
                    // or just include instructiontransformer
                    new TrialRunner(clazz, new FrameworkMethod(clazz.getMethod(method.getName(), List.class)), args).run();
                } catch (InstrumentationException e) {
                    throw new GuidanceException(e);
                } catch (GuidanceException e) {
                    throw e;
                } catch (AssumptionViolatedException | TimeoutException e) {
                    totalIgnore++; //TODO track separately? if everything times out...
                    //TODO consider how to get timeout (stop infinite loops) - could use instrumentingclassloader, but that's really slow
                    // could try with thread.stop (works, but deprecated/dangerous)
                    // could move toward running all mutants in parallel with this sort of thing
                } catch (Throwable e) {
                    if (!isExceptionExpected(e.getClass(), expectedExceptions)) {
                        totalFail++;
                    /*if(!mcl.isDead()) {
                        System.out.println("exception for mutant " + mcl + ": " + e.getClass());
                        // e.printStackTrace();
                    }*/
                        mcl.kill();
                        TraceLogger.get().emit(new KillEvent(0, null, 0, mcl)); //temp 0 values
                        fails.add(e);
                    }
                }
                totalRun++; //TODO mutant not killed - indicate?
                ((MutationCoverage) runCoverage).see(mcl);
            }
        }
        //System.out.println("run " + testClass.getName());
        /*writer.write("Totals:\nFailures: " + totalFail + ", Ignored: " + totalIgnore + ", Run: " + totalRun + "\nFailure List:\n");
        for(Throwable fail : fails) {
            writer.write(fail + "\n");
        }*/
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
        long intervalMilliseconds = now.getTime() - lastRefreshTime.getTime();
        if (intervalMilliseconds < STATS_REFRESH_TIME_PERIOD) {
            return;
        }
        long interlvalTrials = numTrials - lastNumTrials;
        long intervalExecsPerSec = interlvalTrials * 1000L / intervalMilliseconds;
        double intervalExecsPerSecDouble = interlvalTrials * 1000.0 / intervalMilliseconds;
        lastRefreshTime = now;
        lastNumTrials = numTrials;
        long elapsedMilliseconds = now.getTime() - startTime.getTime();
        long execsPerSec = numTrials * 1000L / elapsedMilliseconds;

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
                console.printf("#%,d\tNEW\tcov: %,d exec/s: %,d L: %,d\n", numTrials, nonZeroValidCount, intervalExecsPerSec, currentInput.size());
            } else if (!QUIET_MODE) {
                console.printf("\033[2J");
                console.printf("\033[H");
                console.printf(this.getTitle() + "\n");
                if (this.testName != null) {
                    console.printf("Test name:            %s\n", this.testName);
                }
                console.printf("Results directory:    %s\n", this.outputDirectory.getAbsolutePath());
                console.printf("Elapsed time:         %s (%s)\n", millisToDuration(elapsedMilliseconds),
                        maxDurationMillis == Long.MAX_VALUE ? "no time limit" : ("max " + millisToDuration(maxDurationMillis)));
                console.printf("Number of executions: %,d\n", numTrials);
                console.printf("Valid inputs:         %,d (%.2f%%)\n", numValid, numValid * 100.0 / numTrials);
                console.printf("Cycles completed:     %d\n", cyclesCompleted);
                console.printf("Unique failures:      %,d\n", uniqueFailures.size());
                console.printf("Queue size:           %,d (%,d favored last cycle)\n", savedInputs.size(), numFavoredLastCycle);
                console.printf("Current parent input: %s\n", currentParentInputDesc);
                console.printf("Execution speed:      %,d/sec now | %,d/sec overall\n", intervalExecsPerSec, execsPerSec);
                console.printf("Total coverage:       %,d branches (%.2f%% of map)\n", nonZeroCount, nonZeroFraction);
                console.printf("Valid coverage:       %,d branches (%.2f%% of map)\n", nonZeroValidCount, nonZeroValidFraction);
                console.printf("Total coverage:       %,d mutants\n", ((MutationCoverage) totalCoverage).numCaughtMutants());
                console.printf("Available to Cover:   %,d mutants\n",((MutationCoverage) totalCoverage).numSeenMutants());
            }
        }

        String plotData = String.format("%d, %d, %d, %d, %d, %d, %.2f%%, %d, %d, %d, %.2f, %d, %d, %.2f%%",
                TimeUnit.MILLISECONDS.toSeconds(now.getTime()), cyclesCompleted, currentParentInputIdx,
                numSavedInputs, 0, 0, nonZeroFraction, uniqueFailures.size(), 0, 0, intervalExecsPerSecDouble,
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
