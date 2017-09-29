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
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import edu.berkeley.cs.jqf.fuzz.util.Counter;
import edu.berkeley.cs.jqf.fuzz.util.ProducerHashMap;
import edu.berkeley.cs.jqf.instrument.tracing.events.BranchEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReadEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReturnEvent;
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

    /** Maintains a dynamic calling context (i.e. call stack) */
    protected CallingContext callingContext = new CallingContext();

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

        // Ensure that calling context is empty
        assert(callingContext.isEmpty());

        // Delegate input generation to parent
        return super.hasInput();
    }

    //private PrintWriter trace = new PrintWriter(new FileOutputStream("trace.log"), true);
    //private PrintWriter scores = new PrintWriter(new FileOutputStream("scores.log"), true);

    @Override
    protected void handleEvent(TraceEvent e) {
        //trace.println(e.toString());
        if (e instanceof BranchEvent) {
            BranchEvent b = (BranchEvent) e;
            // Map branch IID to first half of the tracebits map
            int iid = b.isTaken() ? b.getIid() : -b.getIid();
            int edgeIdx = iidToEdgeIdx(iid, COVERAGE_MAP_SIZE/2);

            // Increment the 8-bit branch counter
            incrementTraceBits(edgeIdx);
        } else if (e instanceof ReadEvent) {
            ReadEvent read = (ReadEvent) e;
            // Get memory location that was accessed
            int memoryLocation =
                    hashMemorylocation(read.getObjectId(), read.getField());
            // Get AEC for read operation
            int aec = getAyclicExecutionContextForEvent(read);

            // Map memory access to AEC
            memoryAccesses.get(aec).increment(memoryLocation);


        } else if (e instanceof CallEvent) {
            // Push to calling context
            callingContext.push((CallEvent) e);
        } else if (e instanceof ReturnEvent) {
            // Pop from calling context
            callingContext.pop();
        }
    }


    @Override
    public void handleResult(Result result, Throwable error) {
        // Wait for calling context to be empty
        // (i.e. all AECs are processed)
        while (!callingContext.isEmpty());


        // Compute redundancy scores for all memory accesses and add
        // 8-bit quantized values to "coverage" map
        for (int aec : memoryAccesses.keySet()) {
            double redundancyScore =
                    computeRedundancyScore(memoryAccesses.get(aec).values());

            if (redundancyScore > 0.0) {
                // Discretize the score to a 8-bit value
                byte redundancyByte = (byte) discretizeScore(redundancyScore);

                // Get an index in the upper half of the tracebits map
                int idx = COVERAGE_MAP_SIZE / 2 + iidToEdgeIdx(aec, COVERAGE_MAP_SIZE / 2);

                // Add mapping to trace bits
                traceBits[idx] = redundancyByte;
                // scores.println(String.format("idx = %d, score = %f, value = %d", idx, redundancyScore, redundancyByte));
            }

        }

        // Delegate feedback-sending to parent
        super.handleResult(result, error);
        //scores.println("----");
    }

    protected int hashMemorylocation(int objectId, String field) {
        return field.hashCode() * 31 + objectId;
    }

    boolean optimized = true;

    protected int getAyclicExecutionContextForEvent(TraceEvent e) {
        int aecHash = optimized ?
                callingContext.fastComputeAecHash(e) :
                callingContext.computeAcyclicExecutionContextHash(e);

        return aecHash;
    }

    /**
     * Computes a "redundancy score" for memory accesses at some program
     * location or AEC.
     *
     * The redundancy score formula is chosen such that the value is high
     * when many memory locations are accessed many times each. For a total
     * of N^2 accesses, the score is maximized when N items are accessed N
     * times each. The score is zero when either all items are accessed just
     * once or when only one item is accessed always.
     *
     * @param accessCounts A collection of access counts,
     *                     one positive integer for each memory access
     * @return     the redundancy score
     */
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



    protected class CallingContext {

        protected class Frame {
            final CallEvent call;
            final Frame parent;
            boolean firstInvocation;
            int aecHash;

            Frame(CallEvent call, Frame parent) {
                this.call = call;
                this.parent = parent;
            }

            void precomputeAecHash(Frame acyclicParent) {
                this.aecHash = acyclicParent.aecHash * 31 + this.call.getIid();
            }

        }

        Map<String, Frame> firstInvocations = new HashMap<>();

        Deque<Frame> callStack = new ArrayDeque<>();

        private volatile boolean empty = true;

        public void push(CallEvent callEvent) {
            // Create a new stack frame for this call
            Frame frame = new Frame(callEvent, callStack.peek());

            // If this is the first invocation of a method,
            // then remember this frame (and mark it as a first)
            String methodName = callEvent.getInvokedMethod();
            if (!firstInvocations.containsKey(methodName)) {
                firstInvocations.put(methodName, frame);
                frame.firstInvocation = true;
                // Pre-compute AEC hash
                if (frame.parent != null) {
                    String callingMethod = frame.parent.call.getInvokedMethod();
                    frame.precomputeAecHash(firstInvocations.get(callingMethod));
                }
            }


            // Push the stack frame onto the call stack
            callStack.push(frame);

            // This makes us non-empty
            empty = false;
        }


        public void pop() {
            // Remove frame from call stack
            Frame frame = callStack.pop();

            // If this was the first invocation of the method, remove
            // the entry from the `firstInvoker` map too
            if (frame.firstInvocation) {
                firstInvocations.remove(frame.call.getInvokedMethod());
            }

            // Sanity check: We can't have more first invokers than actual frames
            assert(callStack.size() >= firstInvocations.size());

            if (callStack.size() == 0) {
                empty = true;
            }

        }

        public boolean isEmpty() {
            return empty;
        }

        public String getExecutionContext(TraceEvent e) {
            // At least one frame must be on the stack for this operation
            assert(!callStack.isEmpty());

            // Build the EC by walking down the call stack
            String str = "";
            for (Frame frame : callStack) {
                str += String.format("%s(%s:%d)\n",
                        trimMethodNameOfDesc(frame.call.getInvokedMethod()), e.getFileName(), e.getLineNumber());
                e = frame.call;
            }

            return str;

        }

        public String getAcyclicExecutionContext(TraceEvent e) {
            // At least one frame must be on the stack for this operation
            assert(!callStack.isEmpty());

            // Build the AEC by walking back the `firstInvocation` chain
            String str = "";
            Frame frame = callStack.peek();
            while (frame != null) {
                str += String.format("%s(%s:%d)\n",
                        trimMethodNameOfDesc(frame.call.getInvokedMethod()), e.getFileName(), e.getLineNumber());
                Frame firstInvocationFrame = firstInvocations.get(frame.call.getInvokedMethod());
                e = firstInvocationFrame.call;
                frame = firstInvocationFrame.parent;
            }

            return str;

        }

        public int fastComputeAecHash(TraceEvent e) {
            // At least one frame must be on the stack for this operation
            assert(!callStack.isEmpty());

            // Get the stack frame corresponding to the first call of the current method
            Frame top = callStack.peek();
            Frame firstInvocationOfTopMethod = firstInvocations.get(top.call.getInvokedMethod());

            // Compute AEC hash of current event
            return firstInvocationOfTopMethod.aecHash * 31 + e.getIid();

        }


        public int computeAcyclicExecutionContextHash(TraceEvent e) {
            // At least one frame must be on the stack for this operation
            assert(!callStack.isEmpty());

            // Collect the AEC call sites by walking back the `firstInvocation` chain
            Frame frame = callStack.peek();
            Deque<Integer> iids = new ArrayDeque<>();
            while (frame != null) {
                iids.addFirst(e.getIid());
                Frame firstInvocationFrame = firstInvocations.get(frame.call.getInvokedMethod());
                e = firstInvocationFrame.call;
                frame = firstInvocationFrame.parent;
            }

            // Compute the hash
            int hash = 0;
            for (int iid : iids) {
                hash = 31 * hash + iid;
            }

            return hash;

        }

        private String trimMethodNameOfDesc(String methodName) {
            return methodName.substring(0, methodName.indexOf('('));
        }


    }

}
