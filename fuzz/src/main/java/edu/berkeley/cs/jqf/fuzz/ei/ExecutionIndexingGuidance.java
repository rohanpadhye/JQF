/*
 * Copyright (c) 2017, University of California, Berkeley
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
package edu.berkeley.cs.jqf.fuzz.ei;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.util.Coverage;
import edu.berkeley.cs.jqf.fuzz.util.ProducerHashMap;
import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReturnEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEventVisitor;

import static java.lang.Math.ceil;
import static java.lang.Math.log;

/**
 * A guidance that performs coverage-guided fuzz testing, where inputs
 * are represented not as sequences of bytes but instead as maps of
 * execution indexes to bytes.
 *
 * <p>Whenever the input generator for a test requests a new byte, the
 * execution index of that event is used to query a value in the input
 * map. This representation retains much more structure of the input
 * than a simple linear sequence.</p>
 *
 * @author Rohan Padhye
 */
public class ExecutionIndexingGuidance implements Guidance, TraceEventVisitor {

    // Currently, we only support single-threaded applications
    // This field is used to ensure that
    private Thread appThread;

    /** The last event handled by this guidance */
    private TraceEvent lastEvent;

    /** The execution indexing logic. */
    private ExecutionIndexingState eiState;

    /** A pseudo-random number generator for generating fresh values. */
    private Random random = new Random();

    // ------------ ALGORITHM BOOKKEEPING ------------

    /** The total number of trials to run. */
    private final long maxTrials;

    /** The number of trials completed. */
    private long numTrials = 0;

    /** The directory where saved inputs are written. */
    private final File outputDirectory;

    /** Set of saved inputs to fuzz. */
    private ArrayList<Input> savedInputs = new ArrayList<>();

    /** Queue of seeds to fuzz. */
    private Deque<SeedInput> seedInputs = new ArrayDeque<>();

    /** Current input that's running -- valid after getInput() and before handleResult(). */
    private Input currentInput;

    /** Index of currentInput in the savedInputs -- valid after seeds are processed (OK if this is inaccurate). */
    private int currentParentInputIdx = 0;

    /** Number of mutated inputs generated from currentInput. */
    private int numChildrenGeneratedForCurrentParentInput = 0;

    /** Number of cycles completed (i.e. how many times we've reset currentParentInputIdx to 0. */
    private int cyclesCompleted = 0;

    /** Coverage statistics for a single run. */
    private Coverage runCoverage = new Coverage();

    /** Cumulative coverage statistics. */
    private Coverage totalCoverage = new Coverage();

    /** The maximum number of keys covered by any single input found so far. */
    private int maxCoverage = 0;

    /** A mapping of coverage keys to inputs that are responsible for them. */
    private Map<Object, Input> responsibleInputs = new HashMap<>(totalCoverage.size());

    /**
     * A map of execution contexts (call stacks) to locations in saved inputs with those contexts.
     *
     * This is a nifty data structure for quickly finding candidates for input splicing.
     */
    private Map<ExecutionContext, ArrayList<InputLocation>> ecToInputLoc
            = new ProducerHashMap<>(() -> new ArrayList<>());

    // ---------- LOGGING / STATS OUTPUT ------------

    /** Whether to print log statements to stderr (debug option; manually edit). */
    private final boolean verbose = true;

    /** A system console, which is non-null only if STDOUT is a console. */
    private final Console console = System.console();

    /** Time since this guidance instance was created. */
    private final Date startTime = new Date();

    /** Time at last stats refresh. */
    private Date lastRefreshTime = startTime;

    /** Total execs at last stats refresh. */
    private long lastNumTrials = 0;

    /** Minimum amount of time (in millis) between two stats refreshes. */
    private static final long STATS_REFRESH_TIME_PERIOD = 300;

    /** The file where log data is written. */
    private File logFile;

    /** The file where saved plot data is written. */
    private File statsFile;

    // ------------- FUZZING HEURISTICS ------------

    /** Whether to use real execution indexes as opposed to flat numbering (debug option; manually edit). */
    private static final boolean DISABLE_EXECUTION_INDEXING = Boolean.getBoolean("jqf.ei.DISABLE_EXECUTION_INDEXING");

    /** Max input size to generate. */
    private static final int MAX_INPUT_SIZE = Integer.getInteger("jqf.ei.MAX_INPUT_SIZE", 1024);

    /** Baseline number of mutated children to produce from a given parent input. */
    private static final int NUM_CHILDREN_BASELINE = 50;

    /** Multiplication factor for number of children to produce for favored inputs. */
    private static final int NUM_CHILDREN_MULTIPLIER_FAVORED = 20;

    /** Mean number of mutations to perform in each round. */
    private static final double MEAN_MUTATION_COUNT = 8.0;

    /** Mean number of contiguous bytes to mutate in each mutation. */
    private static final double MEAN_MUTATION_SIZE = 4.0; // Bytes

    /** Max number of contiguous bytes to splice in from another input during the splicing stage. */
    private static final int MAX_SPLICE_SIZE = 64; // Bytes

    /** Whether to splice only at/from locations with a 1-count suffix. */
    private static final boolean ONLY_SPLICE_ONE_SUFFIX = false;

    /** Whether to save inputs that only add new coverage bits (but no new responsibilities). */
    private static final boolean SAVE_NEW_COUNTS = true;

    /** Whether to steal responsibility from old inputs (this increases computation cost). */
    private static final boolean STEAL_RESPONSIBILITY = Boolean.getBoolean("jqf.ei.STEAL_RESPONSIBILITY");


    /**
     * Creates a new execution-index-parametric guidance.
     *
     * @param maxTrials the max number of trials to run
     */
    public ExecutionIndexingGuidance(long maxTrials, File outputDirectory) throws IOException {
        this.maxTrials = maxTrials;
        this.outputDirectory = outputDirectory;
        prepareOutputDirectory();
    }

    public ExecutionIndexingGuidance(long maxTrials, File outputDirectory, File... seedInputFiles) throws IOException {
        this(maxTrials, outputDirectory);
        for (File seedInputFile : seedInputFiles) {
            seedInputs.add(new SeedInput(seedInputFile));
        }
    }

    private void prepareOutputDirectory() throws IOException {

        // Create the output directory if it does not exist
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                throw new IOException("Could not create output directory" +
                        outputDirectory.getAbsolutePath());
            }
        }

        // Make sure we can write to output directory
        if (!outputDirectory.isDirectory() || !outputDirectory.canWrite()) {
            throw new IOException("Output directory is not a writable directory: " +
                    outputDirectory.getAbsolutePath());
        }

        // Delete everything in the output directory (for cases where we re-use an existing dir)
        for (File file : outputDirectory.listFiles()) {
            file.delete(); // We do not check if this was successful
        }

        this.statsFile = new File(outputDirectory, "plot_data");
        this.logFile = new File(outputDirectory, "ei.log");

        appendLineToFile(statsFile,"# unix_time, cycles_done, cur_path, paths_total, pending_total, " +
                "pending_favs, map_size, unique_crashes, unique_hangs, max_depth, execs_per_sec");


    }

    private void appendLineToFile(File file, String line) throws GuidanceException {
        try (PrintWriter out = new PrintWriter(new FileWriter(file, true))) {
            out.println(line);
        } catch (IOException e) {
            throw new GuidanceException(e);
        }

    }

    private void infoLog(String str, Object... args) {
        if (verbose) {
            String line = String.format(str, args);
            if (logFile != null) {
                appendLineToFile(logFile, line);

            } else {
                System.err.println(line);
            }
        }
    }

    // Call only if console exists
    private void displayStats() {
        assert (console != null);

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
        long elapsedSeconds = TimeUnit.MILLISECONDS.toSeconds(elapsedMilliseconds % 60_000);
        long elapsedMinutes = TimeUnit.MILLISECONDS.toMinutes(elapsedMilliseconds);
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

        console.printf("\033[2J");
        console.printf("\033[H");
        console.printf("JQF: ExecutionIndexingGuidance\n");
        console.printf("------------------------------\n");
        console.printf("Elapsed time:         %d min %d sec\n", elapsedMinutes, elapsedSeconds);
        console.printf("Cycles completed:     %d\n", cyclesCompleted);
        console.printf("Queue size:           %d\n", savedInputs.size());
        console.printf("Current parent input: %s\n", currentParentInputDesc);
        console.printf("Number of executions: %d\n", numTrials);
        console.printf("Execution speed:      %d/sec now | %d/sec overall\n", intervalExecsPerSec, execsPerSec);
        console.printf("Covered branches:     %d (%.2f%% of map)\n", nonZeroCount, nonZeroFraction);

        String plotData = String.format("%d, %d, %d, %d, %d, %d, %.2f%%, %d, %d, %d, %.2f",
                TimeUnit.MILLISECONDS.toSeconds(now.getTime()), cyclesCompleted, currentParentInputIdx,
                savedInputs.size(), 0, 0, nonZeroFraction, 0, 0, 0, intervalExecsPerSecDouble);
        appendLineToFile(statsFile, plotData);

    }

    private int getTargetChildrenForParent(Input parentInput) {
        // Baseline is a constant
        int target = NUM_CHILDREN_BASELINE;

        // We like inputs that cover many things, so scale with fraction of max
        if (maxCoverage > 0) {
            target = (NUM_CHILDREN_BASELINE * parentInput.nonZeroCoverage) / maxCoverage;
        }

        // We absolutey love favored inputs, so fuzz them more
        if (parentInput.isFavored()) {
            target = target * NUM_CHILDREN_MULTIPLIER_FAVORED;
        }

        return target;
    }

    private void completeCycle() {
        // Increment cycle count
        cyclesCompleted++;
        infoLog("\n# Cycle " + cyclesCompleted + " completed.");

        // Go over all inputs and do a sanity check (plus log)
        infoLog("Here is a list of favored inputs:");
        int sumResponsibilities = 0;
        for (Input input : savedInputs) {
            if (input.isFavored()) {
                int responsibleFor = input.responsibilities.size();
                infoLog("Input %d is responsible for %d branches", input.id, responsibleFor);
                sumResponsibilities += responsibleFor;
            }
        }
        int totalCoverageCount = totalCoverage.getNonZeroCount();
        infoLog("Total %d branches covered", totalCoverageCount);
        if (sumResponsibilities != totalCoverageCount) {
            throw new AssertionError("Responsibilty mistmatch");
        }

        // Break log after cycle
        infoLog("\n\n\n");
    }

    @Override
    public InputStream getInput() throws GuidanceException {
        // Clear coverage stats for this run
        runCoverage.clear();

        // Reset execution index state
        eiState = new ExecutionIndexingState();

        // Choose an input to execute based on state of queues
        if (!seedInputs.isEmpty()) {
            // First, if we have some specific seeds, use those
            currentInput = seedInputs.removeFirst();

            // Hopefully, the seeds will lead to new coverage and be added to saved inputs

        } else if (savedInputs.isEmpty()) {
            // If no seeds given try to start with something random
            if (numTrials > 100) {
                throw new GuidanceException("Too many trials without coverage; " +
                        "likely all assumption violations");
            }
            currentInput = new Input();
        } else {
            // The number of children to produce is determined by how much of the coverage
            // pool this parent input hits
            Input currentParentInput = savedInputs.get(currentParentInputIdx);
            int targetNumChildren = getTargetChildrenForParent(currentParentInput);
            if (numChildrenGeneratedForCurrentParentInput >= targetNumChildren) {
                // Select the next saved input to fuzz
                currentParentInputIdx = (currentParentInputIdx + 1) % savedInputs.size();

                // Count cycles
                if (currentParentInputIdx == 0) {
                    completeCycle();
                }

                numChildrenGeneratedForCurrentParentInput = 0;
            }
            Input parent = savedInputs.get(currentParentInputIdx);

            // Fuzz it to get a new input
            currentInput = parent.fuzz(random, ecToInputLoc);
            numChildrenGeneratedForCurrentParentInput++;
        }


        // Return an input stream that uses the EI map
        return new InputStream() {
            int bytesRead = 0;

            @Override
            public int read() throws IOException {

                // lastEvent must not be null
                if (lastEvent == null) {
                    throw new IOException("Could not compute execution index; no instrumentation?");
                }

                // Get the execution index of the last event
                ExecutionIndex executionIndex = DISABLE_EXECUTION_INDEXING ?
                        new ExecutionIndex(new int[]{0, bytesRead}) :
                        eiState.getExecutionIndex(lastEvent);

                // Attempt to get a value from the map, or else generate a random value
                int value = currentInput.getOrGenerateFresh(executionIndex, random);

                // Keep track of how many bytes were read in this input
                bytesRead++;

                return value;
            }
        };
    }

    @Override
    public boolean hasInput() {
        return this.numTrials < this.maxTrials;
    }

    @Override
    public void handleResult(Result result, Throwable error) throws GuidanceException {
        // Increment run count
        this.numTrials++;

        if (result == Result.SUCCESS) {

            // Compute a list of keys for which this input can assume responsiblity.
            // Newly covered branches are always included.
            // Existing branches *may* be included, depending on the heuristics used.
            Set<Object> responsibilities = computeResponsibilities();

            // Update total coverage
            boolean coverageBitsUpdated = totalCoverage.updateBits(runCoverage);

            // Coverage after
            int nonZeroAfter = totalCoverage.getNonZeroCount();
            if (nonZeroAfter > maxCoverage) {
                maxCoverage = nonZeroAfter;
            }

            // Possibly save input
            if (responsibilities.size() > 0 || (SAVE_NEW_COUNTS && coverageBitsUpdated)) {
                // Trim input (remove unused keys)
                currentInput.gc();

                // It must still be non-empty
                assert(currentInput.valuesMap.size() > 0);

                infoLog("Saving new input (at run %d): " +
                                "input #%d " +
                                "of size %d; " +
                                "total coverage = %d",
                        numTrials,
                        savedInputs.size(),
                        currentInput.valuesMap.size(),
                        nonZeroAfter);

                // Save input to queue and to disk
                try {
                    saveCurrentInput(responsibilities);
                } catch (IOException e) {
                    throw new GuidanceException(e);
                }

            }
        } else if (result == Result.FAILURE) {
            String msg = error.getMessage();
            infoLog("Found crash: " + error.getClass() + " - " + (msg != null ? msg : ""));
        }

        if (console != null) {
            displayStats();
        }

    }


    // Compute a set of branches for which the current input may assume responsibility
    private Set<Object> computeResponsibilities() {
        Set<Object> result = new HashSet<>();

        // This input is responsible for all new coverage
        Collection<?> newCoverage = runCoverage.computeNewCoverage(totalCoverage);
        if (newCoverage.size() > 0) {
            result.addAll(newCoverage);
        }

        // Perhaps it can also steal responsibility from other inputs
        if (STEAL_RESPONSIBILITY) {
            int currentNonZeroCoverage = runCoverage.getNonZeroCount();
            Set<?> covered = new HashSet<>(runCoverage.getCovered());
            for (Input candidate : savedInputs) {
                Set<?> responsibilities = candidate.responsibilities;

                // Candidates with no responsibility are not interesting
                if (responsibilities.isEmpty()) {
                    continue;
                }

                // To avoid thrashing, only consider candidates with strictly
                // smaller total coverage (implying that responsibility can only
                // be stolen by an input that covers a larger set).
                if (candidate.nonZeroCoverage >= currentNonZeroCoverage) {
                    continue;
                }

                // Check if we can steal all responsibilities from candidate
                boolean canSteal = true;
                for (Object b : responsibilities) {
                    if (covered.contains(b) == false) {
                        // Cannot steal if this input does not cover something
                        // that the candidate is responsible for
                        canSteal = false;
                        break;
                    }
                }
                // If all of candidate's responsibilities are covered by the
                // current input, then it can completely subsume the candidate
                if (canSteal) {
                    result.addAll(responsibilities);
                }
            }
        }

        return result;
    }

    private void saveCurrentInput(Set<Object> responsibilities) throws IOException {
        // First, save to queue
        savedInputs.add(currentInput);

        // Second, save to disk
        int newInputIdx = savedInputs.size()-1;
        String saveFileName = String.format("id:%06d,%s", newInputIdx, currentInput.desc);
        File outputFile = new File(outputDirectory, saveFileName);
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            for (Object key : currentInput.orderedKeys) {
                int b = currentInput.valuesMap.get(key);
                assert (b >= 0 && b < 256);
                out.write(b);
            }
        }

        // Third, store basic book-keeping data
        currentInput.id = newInputIdx;
        currentInput.saveFile = outputFile;
        currentInput.coverage = new Coverage(runCoverage);
        currentInput.nonZeroCoverage = runCoverage.getNonZeroCount();
        currentInput.offspring = 0;
        savedInputs.get(currentParentInputIdx).offspring += 1;

        // Fourth, assume responsibility for branches
        currentInput.responsibilities = responsibilities;
        for (Object b : responsibilities) {
            // If there is an old input that is responsible,
            // subsume it
            Input oldResponsible = responsibleInputs.get(b);
            if (oldResponsible != null) {
                oldResponsible.responsibilities.remove(b);
                infoLog("-- Stealing responsibility for %s from input %d", b, oldResponsible.id);
            } else {
                infoLog("-- Assuming new responsibility for %s", b);
            }
            // We are now responsible
            responsibleInputs.put(b, currentInput);
        }

        // Fifth, map executions to input locations for splicing
        for (int offset = 0; offset < currentInput.size(); offset++) {
            ExecutionIndex ei = currentInput.orderedKeys.get(offset);
            ExecutionContext ec = new ExecutionContext(ei);
            if (ONLY_SPLICE_ONE_SUFFIX && ei.oneSuffixSize() == 0) {
                continue;
            }
            ecToInputLoc.get(ec).add(new InputLocation(currentInput, offset));
        }

    }


    @Override
    public Consumer<TraceEvent> generateCallBack(Thread thread) {
        if (appThread != null) {
            throw new IllegalStateException(ExecutionIndexingGuidance.class +
                " only supports single-threaded apps at the moment");
        }
        appThread = thread;

        return this::handleEvent;
    }

    private void handleEvent(TraceEvent e) {
        // Set last event to this event
        lastEvent = e;

        // Update execution indexing logic
        if (!DISABLE_EXECUTION_INDEXING) {
            e.applyVisitor(this);
        }

        // Collect totalCoverage
        runCoverage.handleEvent(e);
    }

    @Override
    public void visitCallEvent(CallEvent c) {
        eiState.pushCall(c);
    }

    @Override
    public void visitReturnEvent(ReturnEvent r) {
        eiState.popReturn(r);
    }

    /**
     * Returns a reference to the coverage statistics.
     * @return a reference to the coverage statistics
     */
    public Coverage getTotalCoverage() {
        return totalCoverage;
    }


    /**
     * A candidate test input represented as a map from execution indices
     * to integer values.
     *
     * <p>When a quickcheck-like generator requests a new ``random'' byte,
     * the current execution index is used to retrieve the input from
     * this input map (a fresh value is generated and stored in the map
     * if the key is not mapped).</p>
     *
     * <p>Inputs should not be publicly mutable. The only way to mutate
     * an input is via the {@link #fuzz} method which produces a new input
     * object with some values mutated.</p>
     */
    public static class Input {

        /** A map from execution indexes to the byte (0-255) to be returned at that index. */
        protected LinkedHashMap<ExecutionIndex, Integer> valuesMap;

        /**
         * A list of execution indexes that are actually requested by the test program when
         * executed with this input.
         *
         * <p>This list is initially empty, and is populated at the end of the run, after which
         * it is frozen. The list of keys are in order of their occurrence in the execution
         * trace and can therefore be used to serialize the map into a sequence of bytes.</p>
         *
         */
        protected ArrayList<ExecutionIndex> orderedKeys = new ArrayList<>();

        /**
         * Whether this input has been executed.
         *
         * When this field is {@code false}, the field {@link #orderedKeys}
         * is not yet populated and must not be used. When this field is {@code true},
         * the input should be considered immutable and neither {@link #orderedKeys} nor
         * {@link #valuesMap} must be modified.
         */
        protected boolean executed = false;

        /**
         * The file where this input is saved.
         *
         * <p>This field is null for inputs that are not saved.</p>
         */
        private File saveFile = null;

        /**
         * An ID for a saved input.
         *
         * <p>This field is -1 for inputs that are not saved.</p>
         */
        private int id;

        /**
         * The description for this input.
         *
         * <p>This field is modified by the construction and mutation
         * operations.</p>
         */
        private String desc;

        /**
         * The run coverage for this input, if the input is saved.
         *
         * <p>This field is null for inputs that are not saved.</p>
         */
        private Coverage coverage = null;

        /**
         * The number of non-zero elements in `coverage`.
         *
         * <p>This field is -1 for inputs that are not saved.</p>
         *
         * <p></p>When this field is non-negative, the information is
         * redundant (can be computed using {@link Coverage#getNonZeroCount()}),
         * but we store it here for performance reasons.</p>
         */
        private int nonZeroCoverage = -1;

        /**
         * The number of mutant children spawned from this input that
         * were saved.
         *
         * <p>This field is -1 for inputs that are not saved.</p>
         */
        private int offspring = -1;

        /**
         * The set of coverage keys for which this input is
         * responsible.
         *
         * <p>This field is null for inputs that are not saved.</p>
         *
         * <p>Each coverage key appears in the responsibility set
         * of exactly one saved input, and all covered keys appear
         * in at least some responsibility set. Hence, this list
         * needs to be kept in-sync with {@link #responsibleInputs}.</p>
         */
        private Set<Object> responsibilities = null;

        /**
         * Create an empty input map.
         */
        public Input() {
            valuesMap = new LinkedHashMap<>();
            desc = "random";
        }

        /**
         * Create a copy of an existing input map.
         *
         * @param toClone the input map to clone
         */
        public Input(Input toClone) {
            valuesMap = new LinkedHashMap<>(toClone.valuesMap);
            desc = String.format("src:%06d", toClone.id);
        }

        /**
         * Returns the size of this input, in terms of number of bytes
         * in its value map.
         *
         * @return the size of this input
         */
        public final int size() {
            return valuesMap.size();
        }

        /**
         * Returns the byte mapped by this input at a given offset.
         *
         * @param offset the byte offset in the input
         * @return the byte value at that offset
         *
         * @throws IndexOutOfBoundsException if the offset is negative or
         *      larger than {@link #size()-1}
         * @throws IllegalStateException if this method is called before the input
         *                               has been executed
         */
        public final int getValueAtOffset(int offset) throws IndexOutOfBoundsException, IllegalStateException {
            if (!executed) {
                throw new IllegalStateException("Cannot get with offset before execution");
            }

            // Return the mapping for the execution index queried at the offset
            ExecutionIndex ei = orderedKeys.get(offset);
            return valuesMap.get(ei);
        }


        /**
         * Returns the execution index mapped by this input at a given offset.
         *
         * @param offset the byte offset in the input
         * @return the execution index value at that offset
         *
         * @throws IndexOutOfBoundsException if the offset is negative or
         *      larger than {@link #size()-1}
         * @throws IllegalStateException if this method is called before the input
         *                               has been executed
         */
        public final ExecutionIndex getKeyAtOffset(int offset) throws IndexOutOfBoundsException, IllegalStateException {
            if (!executed) {
                throw new IllegalStateException("Cannot get with offset before execution");
            }

            // Return the execution index queried at the offset
            return orderedKeys.get(offset);
        }


        /**
         * Retrieve a value for an execution index if mapped, else generate
         * a fresh value.
         *
         * @param key    the execution index of the trace event requesting a new byte
         * @param random the PRNG
         * @return the value to return to the quickcheck-like generator
         * @throws IllegalStateException if this method is called after the input
         *                               has been executed
         */
        public int getOrGenerateFresh(ExecutionIndex key, Random random) throws IllegalStateException {
            if (executed) {
                throw new IllegalStateException("Cannot generate fresh values after execution");
            }

            // If we reached a limit, then just return EOF
            if (orderedKeys.size() >= MAX_INPUT_SIZE) {
                return -1;
            }

            // Try to get existing values
            Integer val = valuesMap.get(key);
            // If not, generate a new random value
            if (val == null) {
                val = random.nextInt(256);
                valuesMap.put(key, val);
            }

            // Mark this key as visited
            orderedKeys.add(key);

            return val;
        }


        /**
         * Gets the byte mapped by this input at a given execution index.
         *
         * @param ei the execution index
         * @return the value mapped for this index, or {@code null} if no such mapping exists
         *
         * @throws IndexOutOfBoundsException if the offset is negative or
         *      larger than {@link #size()-1}
         */
        public final Integer getValueAtKey(ExecutionIndex ei) throws IndexOutOfBoundsException {
            return valuesMap.get(ei);
        }

        /**
         * Sets the byte mapped by this input at a given execution index.
         *
         * @param ei  the execution index at which to insert
         * @param val the byte to insert
         *
         * @throws IndexOutOfBoundsException if the offset is negative or
         *      larger than {@link #size()-1}
         * @throws IllegalStateException if this method is called after the input
         *                               has been executed
         */
        public final void setValueAtKey(ExecutionIndex ei, int val) throws IndexOutOfBoundsException, IllegalStateException {
            if (executed) {
                throw new IllegalStateException("Cannot set value before execution");
            }

            valuesMap.put(ei, val);
        }

        /**
         * Trims the input map of all keys that were never actually requested since
         * its construction.
         *
         * <p>Although this operation mutates the underlying object, the effect should
         * not be externally visible (at least as long as the test executions are
         * deterministic).</p>
         */
        public void gc() {
            LinkedHashMap<ExecutionIndex, Integer> newMap = new LinkedHashMap<>();
            for (ExecutionIndex key : orderedKeys) {
                newMap.put(key, valuesMap.get(key));
            }
            valuesMap = newMap;

            // Set the `executed` flag
            executed = true;
        }


        /**
         * Return a new input derived from this one with some values
         * mutated.
         *
         * <p>This method performs one or both of random mutations
         * and splicing.</p>
         *
         * <p>Random mutations are done by performing M
         * mutation operations each on a random contiguous sequence of N bytes,
         * where M and N are sampled from a geometric distribution with mean
         * {@link #MEAN_MUTATION_COUNT} and {@link #MEAN_MUTATION_SIZE}
         * respectively.</p>
         *
         * <p>Splicing is performed by first randomly choosing a location and
         * its corresponding execution context in this input's value map, and then
         * copying a contiguous sequence of up to Z bytes from another input,
         * starting with a location that also maps the same execution context.
         * Here, Z is sampled from a uniform distribution from 0 to
         * {@link #MAX_SPLICE_SIZE}.</p>
         *
         * @param random the PRNG
         * @param ecToInputLoc a map of execution contexts to input locations
         * @return a newly fuzzed input
         */
        public Input fuzz(Random random, Map<ExecutionContext, ArrayList<InputLocation>> ecToInputLoc) {
            // Derive new input from this object as source
            Input newInput = new Input(this);

            // Maybe try splicing
            boolean splicingDone = false;


            // Only splice if we have been provided the ecToInputLoc
            if (ecToInputLoc != null) {

                // TODO: Do we really want splicing to be this frequent?
                if (random.nextBoolean()) {

                    outer: for (int targetAttempt = 1; targetAttempt < 3; targetAttempt++) {

                        // Choose an execution context at which to splice at
                        // Note: We get EI and value from `this` rather than `newInput`
                        // because `this` has already been executed
                        int targetOffset = random.nextInt(valuesMap.size());
                        ExecutionIndex ei = this.getKeyAtOffset(targetOffset);

                        // If only splicing EIs with a one-suffix, we may want to
                        // skip this one
                        if (ONLY_SPLICE_ONE_SUFFIX && ei.oneSuffixSize() == 0) {
                            continue;
                        }

                        ExecutionContext ec = new ExecutionContext(ei);
                        int valueAtTarget = this.getValueAtOffset(targetOffset);

                        // Find a suitable input location to splice from
                        ArrayList<InputLocation> inputLocations = ecToInputLoc.get(ec);

                        // If this is a valid target location, it should also be a valid
                        // source location and exist in the ecToInputLoc map
                        assert (inputLocations.size() > 0);

                        InputLocation inputLocation;

                        // Try a bunch of times
                        for (int attempt = 1; attempt <= 10; attempt++) {

                            // Get a candidate source location with the same execution context
                            inputLocation = inputLocations.get(random.nextInt(inputLocations.size()));
                            Input sourceInput = inputLocation.input;
                            int sourceOffset = inputLocation.offset;

                            // Do not splice with ourselves
                            if (sourceInput == this) {
                                continue;
                            }

                            // Do not splice if the first value is the same in source and target
                            if (sourceInput.getValueAtOffset(sourceOffset) == valueAtTarget) {
                                continue;
                            }

                            // OK, this looks good. Let's splice!
                            int spliceSize = 1 + random.nextInt(MAX_SPLICE_SIZE);
                            int src = sourceOffset;
                            int tgt = targetOffset;
                            int splicedBytes = 0;
                            int srcSize = sourceInput.size();
                            int tgtSize = newInput.size();
                            while (splicedBytes < spliceSize && src < srcSize && tgt < tgtSize) {
                                int val = sourceInput.getValueAtOffset(src);
                                ExecutionIndex key = this.getKeyAtOffset(tgt);
                                newInput.setValueAtKey(key, val);

                                splicedBytes++;
                                src++;
                                tgt++;
                            }
                            splicingDone = true;
                            newInput.desc += String.format(",splice:%06d:%d@%d->%d", sourceInput.id, splicedBytes,
                                    sourceOffset, targetOffset);

                            break outer; // Stop more splicing attempts!

                        }
                    }
                }
            }

            // Maybe do random mutations
            if (splicingDone == false || random.nextBoolean()) {

                // Stack a bunch of mutations
                int numMutations = sampleGeometric(random, MEAN_MUTATION_COUNT);
                newInput.desc += ",havoc:"+numMutations;

                for (int mutation = 1; mutation <= numMutations; mutation++) {

                    // Select a random offset and size
                    int offset = random.nextInt(newInput.valuesMap.size());
                    int mutationSize = sampleGeometric(random, MEAN_MUTATION_SIZE);

                    // desc += String.format(":%d@%d", mutationSize, idx);

                    // Iterate over all entries in the value map
                    Iterator<Map.Entry<ExecutionIndex, Integer>> entryIterator
                            = newInput.valuesMap.entrySet().iterator();
                    for (int i = 0; entryIterator.hasNext(); i++) {
                        Map.Entry<ExecutionIndex, Integer> e = entryIterator.next();
                        // Only mutate `mutationSize` contiguous entries from
                        // the randomly selected `idx`.
                        if (i >= offset && i < (offset + mutationSize)) {
                            // Apply a random mutation
                            int mutatedValue = random.nextInt(256);
                            e.setValue(mutatedValue);
                        }
                    }
                }
            }

            return newInput;

        }

        /**
         * Returns whether this input should be favored for fuzzing.
         *
         * <p>An input is favored if it is responsible for covering
         * at least one branch, or if it has helped produce usable
         * offspring in the past.</p>
         *
         * @return
         */
        public boolean isFavored() {
            return responsibilities.size() > 0;
        }

        private int sampleGeometric(Random random, double mean) {
            double p = 1 / mean;
            double uniform = random.nextDouble();
            return (int) ceil(log(1 - uniform) / log(1 - p));
        }

    }

    public static class SeedInput extends Input {
        final File seedFile;
        final InputStream in;

        public SeedInput(File seedFile) throws IOException {
            this.seedFile = seedFile;
            this.in = new BufferedInputStream(new FileInputStream(seedFile));
        }

        @Override
        public int getOrGenerateFresh(ExecutionIndex key, Random random) {
            int value;
            try {
                value = in.read();
            } catch (IOException e) {
                throw new GuidanceException("Error reading from seed file: " + seedFile.getName(), e);

            }
            if (value >= 0) {
                valuesMap.put(key, value);
                orderedKeys.add(key);
            }
            return value;
        }

        @Override
        public void gc() {
            try {
                in.close();
            } catch (IOException e) {
                throw new GuidanceException("Error closing seed file:" + seedFile.getName(), e);
            }
        }

    }


    static class InputLocation {
        private final Input input;
        private final int offset;

        InputLocation(Input input, int offset) {
            this.input = input;
            this.offset = offset;
        }
    }

}
