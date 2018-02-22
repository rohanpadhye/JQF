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
package edu.berkeley.cs.jqf.fuzz.guidance;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import edu.berkeley.cs.jqf.fuzz.util.Coverage;
import edu.berkeley.cs.jqf.fuzz.util.ExecutionIndex;
import edu.berkeley.cs.jqf.fuzz.util.ExecutionIndexingState;
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

    /** The total number of trials to run. */
    private final long maxTrials;

    /** The number of trials completed. */
    private long numTrials = 0;

    /** Cumulative coverage statistics. */
    private Coverage totalCoverage = new Coverage();

    /** Coverage statistics for a single run. */
    private Coverage runCoverage = new Coverage();

    /** Set of saved inputs to fuzz. */
    private ArrayList<Input> savedInputs = new ArrayList<>();

    /** Queue of seeds to fuzz. */
    private Deque<SeedInput> seedInputs = new ArrayDeque<>();

    /** Current input that's running -- valid after getInput() and before handleResult(). */
    private Input currentInput;

    /** Index of currentInput in the savedInputs -- valid after seeds are processed (OK if this is inaccurate). */
    private int currentInputIdx = 0;

    /** Number of mutated inputs generated from currentInput. */
    private int numChildrenGeneratedForCurrentInput = 0;

    /** Number of cycles completed (i.e. how many times we've reset currentInputIdx to 0. */
    private int cyclesCompleted = 0;

    /** Whether to use real execution indexes as opposed to flat numbering (debug option; manually edit). */
    private final boolean realExecutionIndex = true;

    /** Whether to print log statements to stdout (debug option; manually edit). */
    private final boolean verbose = true;

    /** Max input size to generate. */
    private static final int MAX_INPUT_SIZE = 1024; // TODO: Make this configurable

    /** Number of mutated children to produce from a given parent input. */
    private static final int NUM_CHILDREN = 10;

    /** Mean number of mutations to perform in each round. */
    private static final double MEAN_MUTATION_COUNT = 1.2;

    /** Mean number of contiguous bytes to mutate in each mutation. */
    private static final double MEAN_MUTATION_SIZE = 1.5; // Bytes


    /**
     * Creates a new execution-index-parametric guidance.
     *
     * @param maxTrials the max number of trials to run
     */
    public ExecutionIndexingGuidance(long maxTrials) {
        this.maxTrials = maxTrials;
    }

    public ExecutionIndexingGuidance(long maxTrials, File... seedInputFiles) throws IOException {
        this(maxTrials);
        for (File seedInputFile : seedInputFiles) {
            seedInputs.add(new SeedInput(seedInputFile));
        }
    }

    private void infoLog(String str) {
        if (verbose) {
            System.out.println(str);
        }
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
            if (numChildrenGeneratedForCurrentInput >= NUM_CHILDREN) {
                // Select the next saved input to fuzz
                currentInputIdx = (currentInputIdx + 1) % savedInputs.size();
                numChildrenGeneratedForCurrentInput = 0;
                // Count cycles
                if (currentInputIdx == 0) {
                    cyclesCompleted++;
                    infoLog("Cycle " + cyclesCompleted + " completed.");
                }
            }
            Input parent = savedInputs.get(currentInputIdx);

            // Fuzz it to get a new input
            currentInput = parent.fuzz(random);
            numChildrenGeneratedForCurrentInput++;
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
                ExecutionIndex executionIndex = realExecutionIndex ?
                        eiState.getExecutionIndex(lastEvent) :
                        new ExecutionIndex(new int[]{bytesRead});

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

            // Update total coverage
            boolean newCoverage = totalCoverage.updateBits(runCoverage);

            // Possibly save input
            if (newCoverage) {
                currentInput.gc();
                assert(currentInput.valuesMap.size() > 0);
                savedInputs.add(currentInput);
                infoLog(String.format("Saved new input (at run %d): " +
                        "input #%d " +
                        "of size %d; " +
                        "total coverage = %d",
                        numTrials,
                        savedInputs.size(),
                        currentInput.valuesMap.size(),
                        getTotalCoverage().getNonZeroCount()));
            }
        } else if (result == Result.FAILURE) {
            String msg = error.getMessage();
            infoLog("Found crash: " + error.getClass() + " - " + (msg != null ? msg : ""));
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
        e.applyVisitor(this);

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

        protected SortedMap<ExecutionIndex, Integer> valuesMap;
        protected Collection<ExecutionIndex> requiredKeys = new ArrayList<>();

        /**
         * Create an empty input map.
         */
        public Input() {
            valuesMap = new TreeMap<>();
        }

        /**
         * Create a copy of an existing input map.
         *
         * @param toClone the input map to clone
         */
        public Input(Input toClone) {
            valuesMap = new TreeMap<>(toClone.valuesMap);
        }

        /**
         * Retrieve a value for an execution index if mapped, else generate
         * a fresh value.
         *
         * @param key    the execution index of the trace event requesting a new byte
         * @param random the PRNG
         * @return the value to return to the quickcheck-like generator
         */
        public int getOrGenerateFresh(ExecutionIndex key, Random random) {
            // If we reached a limit, then just return EOF
            if (requiredKeys.size() >= MAX_INPUT_SIZE) {
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
            requiredKeys.add(key);

            return val;
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
            SortedMap<ExecutionIndex, Integer> newMap = new TreeMap<>();
            for (ExecutionIndex key : requiredKeys) {
                newMap.put(key, valuesMap.get(key));
            }
            valuesMap = newMap;
        }


        /**
         * Performs a single contiguous mutation on the current input.
         *
         * <p>The size of the mutation is randomly sampled from a
         * geometric distribution with mean
         * {@link #MEAN_MUTATION_SIZE}.</p>
         *
         * @param random the PRNG
         * @param mutation the unary operation to apply on an integer value
         */
        private void mutate(Random random, UnaryOperator<Integer> mutation) {
            // Select a random offset and size
            int idx = random.nextInt(valuesMap.size());
            int mutationSize = sampleGeometric(MEAN_MUTATION_SIZE, random);

            // Iterate over all entries in the value map
            Iterator<Map.Entry<ExecutionIndex, Integer>> entryIterator
                    = valuesMap.entrySet().iterator();
            for (int i = 0; entryIterator.hasNext(); i++) {
                Map.Entry<ExecutionIndex, Integer> e = entryIterator.next();
                // Only mutate `mutationSize` contiguous entries from
                // the randomly selected `idx`.
                if (i >= idx && i < (idx + mutationSize)) {
                    // Apply the provided mutation operation
                    int mutatedValue = mutation.apply(e.getValue());
                    e.setValue(mutatedValue);
                }
            }
        }

        /**
         * Return a new input derived from this one with some values
         * mutated.
         *
         * <p>This method performs multiple stacked mutations on
         * the input map, where the number of mutations is randomly
         * sampled from a geometric distribution with mean
         * {@link #MEAN_MUTATION_COUNT}</p>
         *
         * @param random the PRNG
         * @return a newly fuzzed input
         */
        public Input fuzz(Random random) {
            // Derive new input from this object as source
            Input newInput = new Input(this);
            // Stack a bunch of mutations
            int numMutations = sampleGeometric(MEAN_MUTATION_COUNT, random);
            for (int i = 0; i < numMutations; i++) {
                newInput.mutate(random, (x) -> random.nextInt(256));
            }
            return newInput;
        }

        private int sampleGeometric(double mean, Random random) {
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
                requiredKeys.add(key);
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

}
