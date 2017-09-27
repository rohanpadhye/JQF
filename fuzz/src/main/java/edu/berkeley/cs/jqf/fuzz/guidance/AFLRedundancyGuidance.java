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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import edu.berkeley.cs.jqf.fuzz.util.Counter;
import edu.berkeley.cs.jqf.fuzz.util.ProducerHashMap;
import edu.berkeley.cs.jqf.instrument.tracing.events.BranchEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReadEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;

/**
 * A front-end that uses AFL for increasing redundancy score
 * of heap-memory access expressions.
 *
 * This class extends {@link AFLGuidance} to additionally provide
 * feedback about memory access redundancy.
 *
 * It overrides {@link #handleEvent} to handle <tt>ReadEvent</tt>s
 * (as well as <tt>CallEvent</tt>s and <tt>ReturnEvent</tt>s for
 * computing acyclic execution contexts).
 *
 * @author Rohan Padhye
 */
public class AFLRedundancyGuidance extends AFLGuidance {

    /** Maps acyclic execution contexts to accessed memory locations. */
    protected Map<Integer, Counter<Integer>> memoryAccesses;

    public AFLRedundancyGuidance(File inputFile, File inPipe, File outPipe) throws IOException {
        super(inputFile, inPipe, outPipe);
    }

    public AFLRedundancyGuidance(String inputFileName, String inPipeName, String outPipeName) throws IOException {
        super(inputFileName, inPipeName, outPipeName);
    }

    @Override
    public boolean hasInput() {
        // Reset memory accesses
        // For unmapped AECs, return a counter (map with 0 as default)
        memoryAccesses = new ProducerHashMap<>(() -> new Counter<>());
        // Delegate input generation to parent
        return super.hasInput();
    }

    @Override
    protected void handleEvent(TraceEvent e) {
        if (e instanceof BranchEvent) {
            // Handle branch coverage in parent
            super.handleEvent(e);
        } else if (e instanceof ReadEvent) {
            ReadEvent read = (ReadEvent) e;
            // Get memory location that was accessed
            int memoryLocation =
                    hashMemorylocation(read.getObjectId(), read.getField());
            // Get current AEC
            int aec = currentAecFor(read.getIid());

            // Map memory access to AEC
            memoryAccesses.get(aec).increment(memoryLocation);


        }
    }

    @Override
    public void handleResult(Result result, Throwable error) {
        // Compute redundancy scores for all memory accesses and add
        // 8-bit quantized values to "coverage" map
        for (int aec : memoryAccesses.keySet()) {
            double redundancyScore =
                    computeRedundancyScore(memoryAccesses.get(aec).values());
            byte redundancyByte = (byte) discretizeScore(redundancyScore);

            // Add mapping to trace bits
            traceBits[aec % COVERAGE_MAP_SIZE] = redundancyByte;
        }

        // Delegate feedback-sending to parent
        super.handleResult(result, error);
    }

    protected int hashMemorylocation(int objectId, String field) {
        return field.hashCode() * 31 + objectId;
    }

    protected int currentAecFor(int iid) {
        // TODO: Compute AEC by maintaining a shadow stack
        return iidToEdgeId(iid);
    }

    public static double computeRedundancyScore(Collection<Integer> accessCounts) {
        double numCounts = accessCounts.size();
        double sumCounts = 0.0;
        for (int count : accessCounts) {
            sumCounts += count;
        }
        double averageCounts = sumCounts / numCounts;
        double score = (averageCounts - 1)*(numCounts - 1)/sumCounts;

        return score;
    }

    /**
     * Discretizes a redundancy score to a byte using constrast scaling.
     *
     * @param score a value between 0.0 and 1.0, inclusive
     * @return      a value between 0 and 255, inclusive
     */
    public static int discretizeScore(double score) {
        return (int) Math.round(255 * (Math.pow(2, score) - 1));
    }

}
