package edu.berkeley.cs.jqf.fuzz.mutation;

import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.guidance.TimeoutException;
import edu.berkeley.cs.jqf.fuzz.junit.TrialRunner;
import edu.berkeley.cs.jqf.instrument.InstrumentationException;
import edu.berkeley.cs.jqf.instrument.tracing.events.KillEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
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
import java.util.function.Consumer;

public class MutationGuidance extends ZestGuidance {
    String[] mutables, immutables;
    CartographyClassLoader cartographyClassLoader;

    //TODO how to populate mutables + immutables? (janala?)
    public MutationGuidance(String testName, Duration duration, File outputDirectory) throws IOException {
        super(testName, duration, outputDirectory);
    }

    public MutationGuidance(String testName, Duration duration, File outputDirectory, File[] seedInputFiles) throws IOException {
        super(testName, duration, outputDirectory, seedInputFiles);
    }

    public MutationGuidance(String testName, Duration duration, File outputDirectory, File seedInputDir) throws IOException {
        super(testName, duration, outputDirectory, seedInputDir);
    }

    @Override
    public void handleResult(Result result, Throwable error) throws GuidanceException {
        this.runStart = null;
        this.numTrials++;
        boolean valid = result == Result.SUCCESS;
        if (valid) {
            numValid++;
        }

        if (result == Result.SUCCESS || (result == Result.INVALID && SAVE_ONLY_VALID == false)) {
            int prevMutants = totalCoverage.numCaughtMutants();
            int validNonZeroBefore = validCoverage.getNonZeroCount();
            boolean coverageBitsUpdated = totalCoverage.updateBits(runCoverage);
            boolean mutantsUpdated = totalCoverage.updateMutants(runCoverage);
            if (valid) {
                validCoverage.updateBits(runCoverage);
            }
            int validNonZeroAfter = validCoverage.getNonZeroCount();
            int newMutants = totalCoverage.numCaughtMutants();

            boolean toSave = false;
            String why = "";
            if (mutantsUpdated) {
                toSave = true;
                why = why + "+mutants";
            }
            if (this.validityFuzzing && validNonZeroAfter > validNonZeroBefore) {
                currentInput.setValid(true);
                toSave = true;
                why = why + "+valid";
            }

            if (toSave) {
                currentInput.gc();

                // It must still be non-empty
                //assert(currentInput.size() > 0) : String.format("Empty input: %s", currentInput.desc);

                // libFuzzerCompat stats are only displayed when they hit new coverage
                if (LIBFUZZER_COMPAT_OUTPUT) {
                    displayStats();
                }

                infoLog("Saving new input (at run %d): " +
                                "input #%d " +
                                "of size %d; " +
                                "total coverage = %d mutants",
                        numTrials,
                        savedInputs.size(),
                        currentInput.size(),
                        newMutants);

                // Save input to queue and to disk
                final String reason = why;
                GuidanceException.wrap(() -> saveCurrentInput(totalCoverage.getMutants(), reason));

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
                //assert(currentInput.size() > 0) : String.format("Empty input: %s", currentInput.desc);

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
    public ClassLoader getClassLoader(String[] classPath, ClassLoader parent) throws MalformedURLException {
        cartographyClassLoader = new CartographyClassLoader(classPath, mutables, immutables, parent);
        return cartographyClassLoader;
    }

    @Override
    public void run(TestClass testClass, FrameworkMethod method, Object[] args) throws Throwable {
        // BufferedWriter writer = null; //TODO
        long totalRun = 0, totalFail = 0, totalIgnore = 0;
        List<Throwable> fails = new ArrayList<>();
        List<Class<?>> expectedExceptions = Arrays.asList(method.getMethod().getExceptionTypes());
        for(MutationInstance mcl : cartographyClassLoader.getCartograph()) {
            try {
                new TrialRunner(Class.forName(testClass.getName(), true, mcl), method, args).run();
            } catch(InstrumentationException e) {
                throw new GuidanceException(e);
            } catch (GuidanceException e) {
                throw e;
            } catch (AssumptionViolatedException | TimeoutException e) {
                totalIgnore++;
            } catch (Throwable e) {
                if (!isExceptionExpected(e.getClass(), expectedExceptions)) {
                    totalFail++; //TODO mutant killed - indicate?
                    fails.add(e);
                }
            }
            totalRun++; //TODO mutant not killed - indicate?
        }
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

}
