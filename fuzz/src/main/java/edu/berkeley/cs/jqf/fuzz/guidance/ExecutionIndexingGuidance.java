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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

import edu.berkeley.cs.jqf.fuzz.util.Coverage;
import edu.berkeley.cs.jqf.fuzz.util.ExecutionIndex;
import edu.berkeley.cs.jqf.fuzz.util.ExecutionIndexingState;
import edu.berkeley.cs.jqf.instrument.tracing.SingleSnoop;
import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReturnEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;

/**
 * @author Rohan Padhye
 */
public class ExecutionIndexingGuidance implements Guidance {

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

    /** Queue of inputs to fuzz. */
    private ArrayList<Input> inputQueue = new ArrayList<>();

    /** Current input thats running -- valid after getInput() and before handleResult(). */
    private Input currentInput;

    /** Whether to use real execution indexes as opposed to flat numbering (for evaluation). */
    private boolean realExecutionIndex = true;

    /**
     * Creates a new execution-index-parametric guidance.
     *
     * @param maxTrials the max number of trials to run
     */
    public ExecutionIndexingGuidance(long maxTrials) {
        this.maxTrials = maxTrials;
    }

    @Override
    public InputStream getInput() throws GuidanceException {
        // Clear coverage stats for this run
        runCoverage.clear();

        // Reset execution index state
        eiState = new ExecutionIndexingState();

        // Create a seed input in the first run; fuzz on others
        if (inputQueue.isEmpty()) {
            if (numTrials > 100) {
                throw new GuidanceException("Too many trials without coverage; " +
                        "likely all assumption violations");
            }
            currentInput = new Input();
        } else {
            // Select a random input from the queue to fuzz
            Input seed = inputQueue.get(random.nextInt(inputQueue.size()));

            // Fuzz it to get a new input
            currentInput = seed.fuzz(random);
        }

        //System.out.println("Current input size = " + currentInput.valuesMap.size());


        // Return an input stream that uses the EI map
        return new InputStream() {
            int bytesRead = 0;

            @Override
            public int read() throws IOException {
                // Sync with shadow thread for events to be handled
                SingleSnoop.waitForQuiescence();

                // lastEvent must not be null
                if (lastEvent == null) {
                    throw new IOException("Could not compute execution index; no instrumentation?");
                }

                // Get the execution index of the last event
                ExecutionIndex executionIndex = realExecutionIndex ?
                        eiState.getExecutionIndex(lastEvent) :
                        new ExecutionIndex(new int[]{bytesRead++});

                // System.out.println("Reading byte at EI: " + executionIndex);

                return currentInput.getOrGenerateFresh(executionIndex, random);
            }
        };
    }

    @Override
    public boolean hasInput() {
        return this.numTrials < this.maxTrials;
    }

    @Override
    public void handleResult(Result result, Throwable error) throws GuidanceException {
        // Wait for everything to be processed
        SingleSnoop.waitForQuiescence();

        // Increment run count
        this.numTrials++;

        if (result == Result.SUCCESS) {

            // Update total coverage
            boolean newCoverage = totalCoverage.updateBits(runCoverage);

            // Possibly add input to queue
            if (newCoverage) {
                currentInput.gc();
                inputQueue.add(currentInput);
                System.out.println(String.format("Added to queue (at run %d): " +
                        "input #%d " +
                        "of size %d; " +
                        "total coverage = %d",
                        numTrials,
                        inputQueue.size(),
                        currentInput.valuesMap.size(),
                        getTotalCoverage().getNonZeroCount()));
            }
        } else if (result == Result.FAILURE) {
            error.printStackTrace();
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
        if (e instanceof CallEvent) {
            eiState.pushCall((CallEvent) e);
        } else if (e instanceof ReturnEvent) {
            eiState.popReturn((ReturnEvent) e);
        }

        // Collect totalCoverage
        runCoverage.handleEvent(e);
    }

    /**
     * Returns a reference to the coverage statistics.
     * @return a reference to the coverage statistics
     */
    public Coverage getTotalCoverage() {
        return totalCoverage;
    }


    public static class Input {

        private Map<ExecutionIndex, Integer> valuesMap;
        private Collection<ExecutionIndex> requiredKeys = new ArrayList<>();

        public Input() {
            valuesMap = new LinkedHashMap<>();
        }

        public Input(Input toClone) {
            valuesMap = new LinkedHashMap<>(toClone.valuesMap);
        }

        public int getOrGenerateFresh(ExecutionIndex key, Random random) {
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

        public void gc() {
            Map<ExecutionIndex, Integer> newMap = new LinkedHashMap<>(valuesMap.size());
            for (ExecutionIndex key : requiredKeys) {
                newMap.put(key, valuesMap.get(key));
            }
            valuesMap = newMap;
        }


        private void mutate(Random random) {
            int idx = random.nextInt(valuesMap.size());
            int i = 0;
            for (Map.Entry<?, Integer> e: valuesMap.entrySet()) {
                if (i == idx) {
                    int val = random.nextInt(256);
                    e.setValue(val);
                    break;
                } else {
                    i++;
                }
            }
        }

        public Input fuzz(Random random) {
            Input newInput = new Input(this);
            newInput.mutate(random);
            return newInput;
        }

    }

}
