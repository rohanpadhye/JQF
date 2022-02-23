/*
 * Copyright (c) 2017-2018 The Regents of the University of California
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
package edu.berkeley.cs.jqf.fuzz.afl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.util.Counter;
import edu.berkeley.cs.jqf.fuzz.util.Hashing;
import edu.berkeley.cs.jqf.fuzz.util.MapOfCounters;
import edu.berkeley.cs.jqf.instrument.tracing.events.AllocEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.BranchEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReadEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReturnEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import org.eclipse.collections.api.list.primitive.IntList;

/**
 * A front-end that uses AFL for increasing performance counters
 * in addition to code coverage.
 *
 * <p>This class extends {@link AFLGuidance} to additionally provide
 * feedback about performance measures such as branch counts
 * or allocation sizes.</p>
 *
 * <p>The type of performance metric used is configured by a system
 * property: <code>jqf.afl.perfFeedbackType</code>, which must have
 * one of the values specified in the enum {@link PerfFeedbackType}.
 * This guidance must be used in accordance with the right run
 * scripts that configure the instrumentation to emit trace events
 * related to events such as heap-memory loads and allocations.</p>
 *
 * <p>This guidance class only works with a
 * <a href="https://github.com/carolemieux/perffuzz">modified version of AFL</a>
 * that is designed to maximize performance counters. It will not
 * work properly with stock AFL since it attempts to send more
 * data to AFL than it usually expects.</p>
 *
 * @author Rohan Padhye
 */
public class PerfFuzzGuidance extends AFLGuidance {

    /** The size of the "performance" map that will be sent to AFL. */
    protected static final int PERF_MAP_SIZE = 1 << 14;

    /** Maps branches to counts */
    protected Counter branchCounts = new Counter(PERF_MAP_SIZE - 1);

    /** Count of total number of branches */
    protected int totalBranchCount;

    /** Maps allocation sites to counts */
    protected Counter allocCounts = new Counter(PERF_MAP_SIZE - 1);

    /** Maps acyclic execution contexts to accessed memory locations. */
    protected MapOfCounters memoryAccesses = new MapOfCounters(PERF_MAP_SIZE - 1, 6151);

    /**
     * Maintains a dynamic calling context (i.e. call stack).
     *
     * <p>Note: We assume there is only a single app thread running.
     * For supporting multiple threads, we would have to store
     * a map from threads to calling contexts.
     *
     * */
    protected CallingContext callingContext = new CallingContext();

    /** Configuration of what feedback to send AFL in second-half of map. */
    public enum PerfFeedbackType {
        REDUNDANCY_SCORES,
        BRANCH_COUNTS,
        TOTAL_BRANCH_COUNT,
        ALLOCATION_COUNTS
    }

    /** The feedback to be used by this guidance instance. */
    private final PerfFeedbackType perfFeedbackType;

    public PerfFuzzGuidance(File inputFile, File inPipe, File outPipe) throws IOException {
        super(inputFile, inPipe, outPipe);
        this.perfFeedbackType = PerfFeedbackType.valueOf(System.getProperty("jqf.afl.perfFeedbackType", "BRANCH_COUNTS"));
        System.out.println(this.perfFeedbackType);
    }

    public PerfFuzzGuidance(String inputFileName, String inPipeName, String outPipeName) throws IOException {
        this(new File(inputFileName), new File(inPipeName), new File(outPipeName));
    }

    @Override
    public InputStream getInput() {
        // Reset counters
        memoryAccesses.clear();
        branchCounts.clear();
        allocCounts.clear();
        totalBranchCount = 0;

        // Ensure that calling context is empty
        assert(callingContext.isEmpty());

        // Delegate input generation to parent
        return super.getInput();
    }

    //private PrintWriter trace = new PrintWriter(new FileOutputStream("trace.log"), true);
    private PrintWriter scores = new PrintWriter(new FileOutputStream("scores.log"), true);

    @Override
    protected void handleEvent(TraceEvent e) {
        //trace.println(e.toString());
        if (e instanceof BranchEvent) {
            BranchEvent b = (BranchEvent) e;

            // Map branch IID to first half of the tracebits map (excluding 0)
            int edgeId = 1 + Hashing.hash1(b.getIid(), b.getArm(), (COVERAGE_MAP_SIZE/2) - 1);

            // Increment the 8-bit branch counter
            incrementTraceBits(edgeId);

            // Increment the fine-grained branch counter
            branchCounts.increment(edgeId);

            // Increment the total branch count (holds max 16 bits)
            totalBranchCount++;

            // Check for possible timeouts every so often
            checkForTimeouts();

        } else if (e instanceof ReadEvent) {
            ReadEvent read = (ReadEvent) e;
            if (perfFeedbackType == PerfFeedbackType.REDUNDANCY_SCORES) {
                // Get memory location that was accessed
                int memoryLocation =
                        hashMemorylocation(read.getObjectId(), read.getField());
                // Get AEC for read operation
                int aec = getAyclicExecutionContextForEvent(read);

                // Map memory access to AEC
                memoryAccesses.increment(aec, memoryLocation);
            }

        } else if (e instanceof CallEvent) {
            // Push to calling context
            callingContext.push((CallEvent) e);

            // Map branch IID to first half of the tracebits map (excluding 0)
            int edgeId = 1 + Hashing.hash(e.getIid(), (COVERAGE_MAP_SIZE/2) - 1);

            // Increment the 8-bit counter
            incrementTraceBits(edgeId);
        } else if (e instanceof ReturnEvent) {
            // Pop from calling context
            callingContext.pop();
        } else if (e instanceof AllocEvent) {
            AllocEvent alloc = (AllocEvent) e;
            if (perfFeedbackType == PerfFeedbackType.ALLOCATION_COUNTS) {
                // Get size of allocation
                int size = alloc.getSize();

                // Increment the fine-grained alloc counter by `size`
                allocCounts.increment(alloc.getIid(), size);
            }
        }
    }

    private void putTotalBranchCountIntoFeedback() {
        // Put the total count into the first slot of the perf map
        feedback.putInt(0, totalBranchCount);
    }


    @Override
    public void handleResult(Result result, Throwable error) {
        // First, communicate the coverage information as usual
        super.handleResult(result, error);

        // Wait for calling context to be empty
        // (i.e. all AECs are processed)
        while (!callingContext.isEmpty());

        // Reset the feedback buffer for the perf info
        clearFeedbackBuffer();

        // Now, communicate the performance perfFeedbackType
        switch (this.perfFeedbackType) {
            case TOTAL_BRANCH_COUNT: {
                // Add the total instruction count
                putTotalBranchCountIntoFeedback();
            }
            break;
            case REDUNDANCY_SCORES: {
                // Compute redundancy scores for all memory accesses and add
                // 8-bit quantized values to "coverage" map
                for (int cidx = 0; cidx < PERF_MAP_SIZE; cidx++) {
                    double redundancyScore =
                            computeRedundancyScore(memoryAccesses.nonZeroCountsAtIndex(cidx));

                    int discreteScore = redundancyScore > 0.0 ? discretizeScore(redundancyScore) : 0;
                    assert (discreteScore >= 0 && discreteScore <= Integer.MAX_VALUE);

                    // Put discrete score into a slot with index `cidx`
                    feedback.putInt(cidx * 4, discreteScore);
                    // scores.println(String.format("idx = %d, score = %f, value = %d (0x%08x)", cidx,
                    //    redundancyScore, discreteScore, discreteScore));

                }
                // Also add the total instruction count
                putTotalBranchCountIntoFeedback();
            }
            break;
            case BRANCH_COUNTS: {
                assert (branchCounts.size() == PERF_MAP_SIZE - 1);
                for (int k = 0; k < branchCounts.size(); k++) {
                    // Put count at offset `k+1` integers into the bitmap
                    // since offset 0 is for the total
                    feedback.putInt((k+1) * 4, branchCounts.getAtIndex(k));
                }
                // Also add the total instruction count
                putTotalBranchCountIntoFeedback();
            }
            break;
            case ALLOCATION_COUNTS: {
                assert (allocCounts.size() == PERF_MAP_SIZE - 1);
                for (int k = 0; k < allocCounts.size(); k++) {
                    // Put count at offset `k+1` integers into the bitmap
                    // since offset 0 is for the total
                    feedback.putInt((k+1) * 4, allocCounts.getAtIndex(k));
                }
            }
            break;
        }

        //scores.println("\n");

        // Send feedback to AFL
        try {
            proxyOutput.write(feedback.array(), 0, PERF_MAP_SIZE * 4);
            proxyOutput.flush();
        } catch (IOException e) {
            everything_ok = false;
        }
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
     * <p>The redundancy score formula is chosen such that the value is high
     * when many memory locations are accessed many times each. For a total
     * of N^2 accesses, the score is maximized when N items are accessed N
     * times each. The score is zero when either all items are accessed just
     * once or when only one item is accessed always.
     *
     * @param accessCounts A collection of access counts,
     *                     one positive integer for each memory access
     * @return     the redundancy score
     */
    public static double computeRedundancyScore(IntList accessCounts) {
        double numCounts = accessCounts.size();
        if (numCounts == 0) {
            return 0.0;
        }
        double sumCounts = 0.0;
        sumCounts = accessCounts.sum();
        double averageCounts = sumCounts / numCounts;
        double score = (averageCounts - 1)*(numCounts - 1)/sumCounts;

        return score;
    }

    /**
     * Discretizes a redundancy score to a 32-bit value.
     *
     * @param score a value between 0.0 and 1.0, inclusive
     * @return      a value between 0 and 2^31-1, inclusive
     */
    public static int discretizeScore(double score) {
        return (int) Math.round(Integer.MAX_VALUE * (Math.pow(2, score) - 1));
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
            String methodName = callEvent.getInvokedMethodName();
            if (!firstInvocations.containsKey(methodName)) {
                firstInvocations.put(methodName, frame);
                frame.firstInvocation = true;
                // Pre-compute AEC hash
                if (frame.parent != null) {
                    String callingMethod = frame.parent.call.getInvokedMethodName();
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
                firstInvocations.remove(frame.call.getInvokedMethodName());
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
                        trimMethodNameOfDesc(frame.call.getInvokedMethodName()), e.getFileName(), e.getLineNumber());
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
                        trimMethodNameOfDesc(frame.call.getInvokedMethodName()), e.getFileName(), e.getLineNumber());
                Frame firstInvocationFrame = firstInvocations.get(frame.call.getInvokedMethodName());
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
            Frame firstInvocationOfTopMethod = firstInvocations.get(top.call.getInvokedMethodName());

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
                Frame firstInvocationFrame = firstInvocations.get(frame.call.getInvokedMethodName());
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
