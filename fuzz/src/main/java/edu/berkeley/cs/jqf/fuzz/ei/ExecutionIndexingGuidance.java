/*
 * Copyright (c) 2019, The Regents of the University of California
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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import edu.berkeley.cs.jqf.fuzz.ei.ExecutionIndex.Prefix;
import edu.berkeley.cs.jqf.fuzz.ei.ExecutionIndex.Suffix;
import edu.berkeley.cs.jqf.fuzz.ei.state.AbstractExecutionIndexingState;
import edu.berkeley.cs.jqf.fuzz.ei.state.FastExecutionIndexingState;
import edu.berkeley.cs.jqf.fuzz.ei.state.JanalaExecutionIndexingState;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FuzzStatement;
import edu.berkeley.cs.jqf.fuzz.util.CoverageFactory;
import edu.berkeley.cs.jqf.fuzz.util.ProducerHashMap;
import edu.berkeley.cs.jqf.instrument.tracing.FastCoverageSnoop;
import edu.berkeley.cs.jqf.instrument.tracing.SingleSnoop;
import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import janala.instrument.FastCoverageListener;
import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

/**
 * A guidance that represents inputs as maps from
 * execution indexes to parameters.
 *
 * @author Rohan Padhye
 */
public class ExecutionIndexingGuidance extends ZestGuidance {

    /** The execution indexing logic. */
    protected AbstractExecutionIndexingState eiState;

    /**
     * A map of execution contexts (call stacks) to locations in saved inputs with those contexts.
     *
     * This is a nifty data structure for quickly finding candidates for input splicing.
     */
    private Map<ExecutionContext, ArrayList<InputLocation>> ecToInputLoc
            = new ProducerHashMap<>(() -> new ArrayList<>());

    /** The thread being instrumented for coverage-guided fuzzing. */
    protected Thread appThread;

    /** The entry point to the test method we are fuzzing. */
    protected String entryPoint;

    /** Whether the the entry point has been encountered in the current run. */
    protected boolean testEntered;

    /** Maps a hash code of coverage bits to an index in savedInputs queue. */
    protected Map<Integer, Integer> coverageHashToSavedInputIdx = new HashMap<>();

    /** Mean number of mutations to perform in each round. */
    protected final double MEAN_MUTATION_COUNT = 8.0;

    /** Mean number of contiguous bytes to mutate in each mutation. */
    protected final double MEAN_MUTATION_SIZE = 4.0; // Bytes

    /** Probability that a standard mutation sets the byte to just zero instead of a random value. */
    protected final double MUTATION_ZERO_PROBABILITY = 0.05;

    /** Max number of contiguous bytes to splice in from another input during the splicing stage. */
    protected final int MAX_SPLICE_SIZE = 64; // Bytes

    /** Whether to splice only in the same sub-tree */
    protected final boolean SPLICE_SUBTREE = Boolean.getBoolean("jqf.ei.SPLICE_SUBTREE");

    /** Probability of splicing in {@link MappedInput#fuzz(Random, Map)} */
    protected final double STANDARD_SPLICING_PROBABILITY = 0.5;

    /** Probability of splicing in {@link MappedInput#getOrGenerateFresh(ExecutionIndex, Random)}  */
    protected final double DEMAND_DRIVEN_SPLICING_PROBABILITY = 0.0;

    /**
     * Constructs a new EI guidance instance with optional duration,
     * optional trial limit, and possibly deterministic PRNG.
     *
     * @param testName the name of test to display on the status screen
     * @param duration the amount of time to run fuzzing for, where
     *                 {@code null} indicates unlimited time.
     * @param trials   the number of trials for which to run fuzzing, where
     *                 {@code null} indicates unlimited trials.
     * @param outputDirectory the directory where fuzzing results will be written
     * @param sourceOfRandomness      a pseudo-random number generator
     * @throws IOException if the output directory could not be prepared
     */
    public ExecutionIndexingGuidance(String testName, Duration duration, Long trials, File outputDirectory, Random sourceOfRandomness) throws IOException {
        super(testName, duration, trials, outputDirectory, sourceOfRandomness);
    }

    /**
     * Constructs a new EI guidance instance with seed input directory and optional
     * duration, optional trial limit, an possibly deterministic PRNG.
     *
     * @param testName the name of test to display on the status screen
     * @param duration the amount of time to run fuzzing for, where
     *                 {@code null} indicates unlimited time.
     * @param trials   the number of trials for which to run fuzzing, where
     *                 {@code null} indicates unlimited trials.
     * @param outputDirectory the directory where fuzzing results will be written
     * @param seedInputDir the directory containing one or more input files to be used as initial inputs
     * @param sourceOfRandomness      a pseudo-random number generator
     * @throws IOException if the output directory could not be prepared
     */
    public ExecutionIndexingGuidance(String testName, Duration duration, Long trials, File outputDirectory, File seedInputDir, Random sourceOfRandomness) throws IOException {
        super(testName, duration, trials, outputDirectory, seedInputDir, sourceOfRandomness);
    }

    /**
     * Creates a new EI guidance instance with seed input files and
     * optional duration.
     *
     * @param testName the name of test to display on the status screen
     * @param duration the amount of time to run fuzzing for, where
     *                 {@code null} indicates unlimited time.
     * @param outputDirectory the directory where fuzzing results will be written
     * @throws IOException if the output directory could not be prepared
     */
    public ExecutionIndexingGuidance(String testName, Duration duration, File outputDirectory, File[] seedFiles) throws IOException {
        super(testName, duration, outputDirectory, seedFiles);
    }

    /** Returns the banner to be displayed on the status screen */
    protected String getTitle() {
        if (blind) {
            return super.getTitle();
        } else {
            return  "Semantic Fuzzing with Execution Indexes\n" +
                    "---------------------------------------\n";
        }
    }

    /** Spawns a new input from thin air (i.e., actually random) */
    @Override
    protected Input<?> createFreshInput() {
        return new MappedInput();
    }

    /**
     * Returns an InputStream that delivers parameters to the generators.
     *
     * Note: The variable `currentInput` has been set to point to the input
     * to mutate.
     */
    @Override
    protected InputStream createParameterStream() {
        // Return an input stream that uses the EI map
        return new InputStream() {
            @Override
            public int read() throws IOException {

                // lastEvent must not be null
                if (eiState.getLastEventIid() == -1) {
                    throw new GuidanceException("Could not compute execution index; no instrumentation?");
                }

                assert currentInput instanceof MappedInput : "This guidance should only mutate MappedInput(s)";

                MappedInput mappedInput = (MappedInput) currentInput;

                // Get the execution index of the last event
                ExecutionIndex executionIndex = eiState.getExecutionIndex(eiState.getLastEventIid());

                // Attempt to get a value from the map, or else generate a random value
                int value = mappedInput.getOrGenerateFresh(executionIndex, random);

                return value;
            }
        };
    }

    @Override
    public InputStream getInput() throws GuidanceException {
        // First, reset execution indexing state
        eiState = CoverageFactory.newEIState();
        if (eiState instanceof FastExecutionIndexingState) {
            FastCoverageSnoop.setFastCoverageListener((FastCoverageListener) eiState);
        }


        // Unmark "test started"
        testEntered = false;

        // Then, do the same logic as ZestGuidance (e.g. returning seeds, mutated inputs, or new input)
        return super.getInput();
    }

    @Override
    public void run(TestClass testClass, FrameworkMethod method, Object[] args) throws Throwable {
        if (this.runCoverage instanceof FastCoverageListener) {
            FastCoverageSnoop.setFastCoverageListener((FastCoverageListener) this.runCoverage);
        }
        super.run(testClass, method, args);
    }

    /**
     * Handles the result of a test execution.
     *
     * This method mostly delegates to the {@link ZestGuidance}, but additionally
     * incorporates some custom logic to support minimization
     */
    @Override
    public void handleResult(Result result, Throwable error) throws GuidanceException {
        int numSavedInputsBefore = savedInputs.size();
        super.handleResult(result, error);

        // Was this a good input?
        if (result == Result.SUCCESS) {
            // Get hash for the current run's coverage
            int coverageHash = runCoverage.hashCode();
            // Was this saved?
            if (savedInputs.size() > numSavedInputsBefore) {
                // If yes, map the hash to the last saved input index
                assert numSavedInputsBefore == savedInputs.size() - 1 : "savedInputs can only grow by 1 at a time";
                coverageHashToSavedInputIdx.put(coverageHash, numSavedInputsBefore);
            } else {
                // If the current input was not saved (maybe no new coverage),
                // then see if it can replace an existing input with save coverage
                if (coverageHashToSavedInputIdx.containsKey(coverageHash)) {
                    int otherIdx = coverageHashToSavedInputIdx.get(coverageHash);
                    Input<?> otherInput = savedInputs.get(otherIdx);

                    // Let's compare input sizes of this input and the other input
                    // But first, we need to run gc on the current input to get the right size
                    currentInput.gc();

                    if (otherInput != null && currentInput.size() < otherInput.size()) {
                        // Bingo! We have the same coverage as someone else but smaller input size :-)
                        infoLog("Minimzation successful! Replacing input %d with %s (size %d ==> %d bytes)",
                                otherIdx, currentInput.desc, otherInput.size(), currentInput.size());

                        // First, replace in saved inputs
                        savedInputs.set(otherIdx, currentInput);

                        // Second, update responsibilities
                        IntIterator otherResponsibilitiesIter = otherInput.responsibilities.intIterator();
                        while(otherResponsibilitiesIter.hasNext()){
                            int b = otherResponsibilitiesIter.next();
                            // Subsume responsibility
                            // infoLog("-- Stealing responsibility for %s from old input %d", b, otherIdx);
                            // We are now responsible
                            responsibleInputs.put(b, currentInput);
                        }
                        currentInput.responsibilities = otherInput.responsibilities;


                        // Third, store basic book-keeping data
                        currentInput.id = otherIdx;
                        currentInput.saveFile = otherInput.saveFile;
                        currentInput.coverage = runCoverage.copy();
                        currentInput.nonZeroCoverage = runCoverage.getNonZeroCount();
                        currentInput.offspring = 0;
                        savedInputs.get(currentParentInputIdx).offspring += 1;

                        // Finally, overwrite the saved input file on disc
                        try {
                            writeCurrentInputToFile(currentInput.saveFile);
                        } catch (IOException e) {
                            throw new GuidanceException(e);
                        }


                        // Note: coverageHashToSavedInputIdx does not need to be updated, as the mapping is the same

                    }
                }
            }
        }
    }


    /** Saves an interesting input to the queue. */
    @Override
    protected void saveCurrentInput(IntHashSet responsibilities, String why) throws IOException {
        // First, do same as Zest
        super.saveCurrentInput(responsibilities, why);

        // Then, map executions to input locations for splicing
        mapEcToInputLoc(currentInput);
    }

    /** Handles the end of fuzzing cycle (i.e., having gone through the entire queue) */
    @Override
    protected void completeCycle() {
        // First, do same as Zest
        super.completeCycle();

        // Then, refresh ecToInputLoc so that subsequent splices are only from favored inputs
        ecToInputLoc.clear();
        for (Input input : savedInputs) {
            if (input.isFavored()) {
                mapEcToInputLoc(input);
            }
        }
    }


    private void mapEcToInputLoc(Input input) {
        if (input instanceof MappedInput) {
            MappedInput mappedInput = (MappedInput) input;
            for (int offset = 0; offset < mappedInput.size(); offset++) {
                ExecutionIndex ei = mappedInput.orderedKeys.get(offset);
                ExecutionContext ec = new ExecutionContext(ei);
                ecToInputLoc.get(ec).add(new InputLocation(mappedInput, offset));
            }
        }

    }


    @Override
    public Consumer<TraceEvent> generateCallBack(Thread thread) {
        if (appThread != null) {
            throw new IllegalStateException(ExecutionIndexingGuidance.class +
                    " only supports single-threaded apps at the moment");
        }
        appThread = thread;
        entryPoint = SingleSnoop.entryPoints.get(thread).replace('.', '/');
        assert entryPoint != null : ExecutionIndexingGuidance.class + " must be able to determine an entry point";

        return this::handleEvent;

    }

    /** Handles a trace event generated during test execution */
    @Override
    protected void handleEvent(TraceEvent e) {
        if (eiState instanceof JanalaExecutionIndexingState) {
            // Update execution indexing logic regardless of whether we are in generator or test method
            e.applyVisitor((JanalaExecutionIndexingState) eiState);
        }

        // Do not handle code coverage unless test has been entered
        if (!testEntered) {
            // Check if this event enters the test method
            if (e instanceof CallEvent) {
                CallEvent callEvent = (CallEvent) e;
                if (callEvent.getInvokedMethodName().startsWith(entryPoint)) {
                    testEntered = true;
                }
            }

            // If test method has not yet been entered, then ignore code coverage
            if (!testEntered) {
                return;
            }
        }

        // Delegate to ZestGuidance for handling code coverage
        super.handleEvent(e);

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
    public class MappedInput extends Input<ExecutionIndex> {

        /**
         * Whether this input has been executed.
         *
         * When this field is {@code false}, the field {@link #orderedKeys}
         * is not yet populated and must not be used. When this field is {@code true},
         * the input should be considered immutable and neither {@link #orderedKeys} nor
         * {@link #valuesMap} must be modified.
         */
        protected boolean executed = false;

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


        private List<InputPrefixMapping> demandDrivenSpliceMap = new ArrayList<>();

        /**
         * Create an empty input map.
         */
        public MappedInput() {
            super();
            valuesMap = new LinkedHashMap<>();
        }

        /**
         * Create a copy of an existing input map.
         *
         * @param toClone the input map to clone
         */
        public MappedInput(MappedInput toClone) {
            super(toClone);
            valuesMap = new LinkedHashMap<>(toClone.valuesMap);
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
         *      larger than {@link #size}()-1
         * @throws IllegalStateException if this method is called before the input
         *                               has been executed
         */
        private final int getValueAtOffset(int offset) throws IndexOutOfBoundsException, IllegalStateException {
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
         *      larger than {@link #size}()-1
         * @throws IllegalStateException if this method is called before the input
         *                               has been executed
         */
        private final ExecutionIndex getKeyAtOffset(int offset) throws IndexOutOfBoundsException, IllegalStateException {
            if (!executed) {
                throw new IllegalStateException("Cannot get with offset before execution");
            }

            // Return the execution index queried at the offset
            return orderedKeys.get(offset);
        }

        private InputPrefixMapping getInputPrefixMapping(ExecutionIndex ei) {
            for (InputPrefixMapping ipm : demandDrivenSpliceMap) {
                if (ei.hasPrefix(ipm.targetPrefix)) {
                    return ipm;
                }
            }
            return null;
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
        @Override
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

            // If not, generate a new value
            if (val == null) {
                InputPrefixMapping ipm;

                // If we have an input prefix mapping for this execution index,
                // then splice from the source input
                if ((ipm = getInputPrefixMapping(key)) != null) {
                    Prefix sourcePrefix = ipm.sourcePrefix;
                    Suffix sourceSuffix = ipm.sourcePrefix.getEi().getSuffixOfPrefix(sourcePrefix);
                    ExecutionIndex sourceEi = new ExecutionIndex(sourcePrefix, sourceSuffix);
                    // The value can be taken from the source
                    val = ipm.sourceInput.getValueAtKey(sourceEi);
                }

                // If we could not splice or were unsuccessful, try to generate a new input
                if (val == null) {
                    if (GENERATE_EOF_WHEN_OUT) {
                        return -1;
                    }
                    if (random.nextDouble() < DEMAND_DRIVEN_SPLICING_PROBABILITY) {
                        // TODO: Find a random inputLocation with same EC,
                        // extract common suffix of sourceEi and targetEi,
                        // and map targetPrefix to sourcePrefix in the IPM


                    } else {
                        // Just generate a random input
                        val = random.nextInt(256);
                    }
                }

                // Put the new value into the map
                assert (val != null);

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
         *      larger than {@link #size}()-1
         */
        protected final Integer getValueAtKey(ExecutionIndex ei) throws IndexOutOfBoundsException {
            return valuesMap.get(ei);
        }

        /**
         * Sets the byte mapped by this input at a given execution index.
         *
         * @param ei  the execution index at which to insert
         * @param val the byte to insert
         *
         * @throws IndexOutOfBoundsException if the offset is negative or
         *      larger than {@link #size}()-1
         * @throws IllegalStateException if this method is called after the input
         *                               has been executed
         */
        protected final void setValueAtKey(ExecutionIndex ei, int val) throws IndexOutOfBoundsException, IllegalStateException {
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
        @Override
        public void gc() {
            LinkedHashMap<ExecutionIndex, Integer> newMap = new LinkedHashMap<>();
            for (ExecutionIndex key : orderedKeys) {
                newMap.put(key, valuesMap.get(key));
            }
            valuesMap = newMap;
            assert valuesMap.size() == orderedKeys.size() : "valuesMap and orderedKeys must be of same size";

            // Set the `executed` flag
            executed = true;
        }

        /**
         * Return a new input derived from this one with some values
         * mutated.
         *
         * Pass-through to {@link #fuzz(Random, Map)}
         *
         * @param random a pseudo-random source
         * @return a mutated input
         */
        @Override
        public Input fuzz(Random random) {
            return fuzz(random, ExecutionIndexingGuidance.this.ecToInputLoc);
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
         * @param ecToInputLoc map of execution contexts to input-location pairs
         * @return a newly fuzzed input
         */
        protected MappedInput fuzz(Random random, Map<ExecutionContext, ArrayList<InputLocation>> ecToInputLoc) {
            // Derive new input from this object as source
            MappedInput newInput = new MappedInput(this);

            // Maybe try splicing
            boolean splicingDone = false;

            // Only splice if we have been provided the ecToInputLoc
            if (ecToInputLoc != null) {

                if (random.nextDouble() < STANDARD_SPLICING_PROBABILITY) {
                    final int MIN_TARGET_ATTEMPTS = 3;
                    final int MAX_TARGET_ATTEMPTS = 6;

                    int targetAttempts = MIN_TARGET_ATTEMPTS;

                    outer: for (int targetAttempt = 1; targetAttempt < targetAttempts; targetAttempt++) {

                        // Choose an execution context at which to splice at
                        // Note: We get EI and value from `this` rather than `newInput`
                        // because `this` has already been executed
                        int targetOffset = random.nextInt(newInput.valuesMap.size());
                        ExecutionIndex targetEi = this.getKeyAtOffset(targetOffset);

                        ExecutionContext targetEc = new ExecutionContext(targetEi);
                        int valueAtTarget = this.getValueAtOffset(targetOffset);

                        // Find a suitable input location to splice from and ignore locations from the same source
                        List<InputLocation> inputLocations = ecToInputLoc
                                .get(targetEc).stream().filter(it -> it.input != this).collect(Collectors.toList());


                        // If this was a bad choice of target, try again without penalty if possible
                        if (inputLocations.size() == 0) {
                            // Try to increase the loop bound a little bit to get another chance
                            targetAttempts = Math.min(targetAttempts+1, MAX_TARGET_ATTEMPTS);
                            continue;
                        }

                        InputLocation inputLocation;

                        // Try a bunch of times
                        for (int attempt = 1; attempt <= 10; attempt++) {

                            // Get a candidate source location with the same execution context
                            inputLocation = inputLocations.get(random.nextInt(inputLocations.size()));
                            MappedInput sourceInput = inputLocation.input;
                            int sourceOffset = inputLocation.offset;


                            // Do not splice with ourselves
                            if (sourceInput == this) {
                                continue;
                            }

                            // Do not splice if the first value is the same in source and target
                            if (sourceInput.getValueAtOffset(sourceOffset) == valueAtTarget) {
                                continue;
                            }

                            int splicedBytes = 0;
                            if (SPLICE_SUBTREE) {
                                // Do not splice if there is no common suffix between EI of source and target
                                ExecutionIndex sourceEi = sourceInput.getKeyAtOffset(sourceOffset);
                                Suffix suffix = targetEi.getCommonSuffix(sourceEi);
                                if (suffix.size() == 0) {
                                    continue;
                                }

                                // Extract the source and target prefixes
                                Prefix sourcePrefix = sourceEi.getPrefixOfSuffix(suffix);
                                Prefix targetPrefix = targetEi.getPrefixOfSuffix(suffix);
                                assert (sourcePrefix.size() == targetPrefix.size());
                                demandDrivenSpliceMap.add(
                                        new InputPrefixMapping(sourceInput, sourcePrefix, targetPrefix));

                                // OK, this looks good. Let's splice!
                                int srcIdx = sourceOffset;
                                while (srcIdx < sourceInput.size()) {
                                    ExecutionIndex candidateEi = sourceInput.getKeyAtOffset(srcIdx);
                                    if (candidateEi.hasPrefix(sourcePrefix) == false) {
                                        // We are no more in the same sub-tree as sourceEi
                                        break;
                                    }
                                    Suffix spliceSuffix = candidateEi.getSuffixOfPrefix(sourcePrefix);
                                    ExecutionIndex spliceEi = new ExecutionIndex(targetPrefix, spliceSuffix);
                                    newInput.valuesMap.put(spliceEi, sourceInput.valuesMap.get(candidateEi));

                                    srcIdx++;
                                }
                                splicedBytes = srcIdx - sourceOffset;
                            } else {

                                int spliceSize = 1 + random.nextInt(MAX_SPLICE_SIZE);
                                int src = sourceOffset;
                                int tgt = targetOffset;
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
                            }

                            // Complete splicing
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
                    // infoLog("[%d] Mutating %d bytes at offset %d", mutation, mutationSize, offset);

                    newInput.desc += String.format("(%d@%d)", mutationSize, offset);

                    boolean setToZero = random.nextDouble() < MUTATION_ZERO_PROBABILITY; // one out of 10 times
                    if (setToZero) {
                        newInput.desc += "=0";
                    }

                    // Iterate over all entries in the value map
                    Iterator<Map.Entry<ExecutionIndex, Integer>> entryIterator
                            = newInput.valuesMap.entrySet().iterator();
                    ExecutionContext ecToMutate = null;
                    for (int i = 0; entryIterator.hasNext(); i++) {
                        Map.Entry<ExecutionIndex, Integer> e = entryIterator.next();
                        // Only mutate `mutationSize` contiguous entries from
                        // the randomly selected `idx`.
                        if (i >= offset && i < (offset + mutationSize)) {
                            ExecutionContext currentEc = new ExecutionContext(e.getKey());
                            if (ecToMutate == null) {
                                ecToMutate = currentEc;
                            } else if (!ecToMutate.equals(currentEc)) {
                                break;
                            }
                            // infoLog("Mutating: %s", e.getKey());
                            // Apply a random mutation
                            int mutatedValue = setToZero ? 0 : random.nextInt(256);
                            e.setValue(mutatedValue);

                        }
                    }
                }
            }

            return newInput;

        }

        @Override
        public Iterator<Integer> iterator() {
            return new Iterator<Integer>() {

                Iterator<ExecutionIndex> keyIt = orderedKeys.iterator();

                @Override
                public boolean hasNext() {
                    return keyIt.hasNext();
                }

                @Override
                public Integer next() {
                    return valuesMap.get(keyIt.next());
                }
            };
        }
    }

    static class InputLocation {
        private final MappedInput input;
        private final int offset;

        InputLocation(MappedInput input, int offset) {
            this.input = input;
            this.offset = offset;
        }
    }

    static class InputPrefixMapping {
        private final MappedInput sourceInput;
        private final Prefix sourcePrefix;
        private final Prefix targetPrefix;

        InputPrefixMapping(MappedInput sourceInput, Prefix sourcePrefix, Prefix targetPrefix) {
            this.sourceInput = sourceInput;
            this.sourcePrefix = sourcePrefix;
            this.targetPrefix = targetPrefix;
        }
    }

    public class MappedSeedInput extends MappedInput {
        final File seedFile;
        final InputStream in;

        public MappedSeedInput(File seedFile) throws IOException {
            super();
            this.seedFile = seedFile;
            this.in = new BufferedInputStream(new FileInputStream(seedFile));
            this.desc = "seed";
        }

        @Override
        public int getOrGenerateFresh(ExecutionIndex key, Random random) {
            int value;
            try {
                value = in.read();
            } catch (IOException e) {
                throw new GuidanceException("Error reading from seed file: " + seedFile.getName(), e);

            }

            // Check for EOF (ideally, should not happen since generators are expected to be deterministic)
            if (value == -1) {
                // Bail if configured to do so
                if (GENERATE_EOF_WHEN_OUT) {
                    // Returned as EOF but not added to the map
                    return -1;
                }

                // More commonly, generate a random value
                value = random.nextInt(256);
            }

            // Populate the value map
            orderedKeys.add(key);
            valuesMap.put(key, value);

            return value;
        }

        @Override
        public void gc() {
            // Set the `executed` flag
            executed = true;

            // Close the seed file
            try {
                in.close();
            } catch (IOException e) {
                throw new GuidanceException("Error closing seed file:" + seedFile.getName(), e);
            }
        }

    }

}
