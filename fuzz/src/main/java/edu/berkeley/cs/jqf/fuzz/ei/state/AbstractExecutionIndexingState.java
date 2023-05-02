package edu.berkeley.cs.jqf.fuzz.ei.state;

import edu.berkeley.cs.jqf.fuzz.ei.ExecutionIndex;
import edu.berkeley.cs.jqf.fuzz.util.Counter;
import edu.berkeley.cs.jqf.fuzz.util.NonZeroCachingCounter;
import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReturnEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEventVisitor;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * A mutable state representing the current call stack with prefix counts,
 * used to compute light-weight execution indexes.
 *
 * @see ExecutionIndex
 *
 * @author Rohan Padhye
 */
public abstract class AbstractExecutionIndexingState {
    private final int COUNTER_SIZE = 6151;
    private final int MAX_SUPPORTED_DEPTH = 1024; // Nothing deeper than this

    private int depth = 0;
    private int lastEventIid = -1;
    private ArrayList<Counter> stackOfCounters = new ArrayList<>();
    private int[] rollingIndex = new int[2*MAX_SUPPORTED_DEPTH];

    public AbstractExecutionIndexingState() {
        // Create a counter for depth = 0
        stackOfCounters.add(new NonZeroCachingCounter(COUNTER_SIZE));
    }

    public AbstractExecutionIndexingState(AbstractExecutionIndexingState eis) {
        depth = eis.depth;
        lastEventIid = eis.lastEventIid;
        for(Counter c : eis.stackOfCounters) {
            stackOfCounters.add(new Counter(c));
        }
        System.arraycopy(eis.rollingIndex, 0, rollingIndex, 0, eis.rollingIndex.length);
    }

    protected void setLastEventIid(int iid) {
        lastEventIid = iid;
    }

    public int getLastEventIid() {
        return lastEventIid;
    }

    public void pushCall(int iid) {
        // Increment counter for call-site (note: this is subject to hash collisions)
        int count = stackOfCounters.get(depth).increment(iid);

        // Add to rolling execution index
        rollingIndex[2*depth] = iid;
        rollingIndex[2*depth + 1] = count;

        // Increment depth
        depth++;

        // Ensure that we do not go very deep
        if (depth >= MAX_SUPPORTED_DEPTH) {
            throw new StackOverflowError("Very deep stack; cannot compute execution index");
        }

        // Push a new counter if it does not exist
        if (depth >= stackOfCounters.size()) {
            stackOfCounters.add(new NonZeroCachingCounter(COUNTER_SIZE));
        }

    }

    public void popReturn(int iid) {
        // We want to pop all indices until we meet
        // the current return function. This is a lazy
        // way to handle exceptions. We only need
        // to do this for FastExecutionIndexingState.
        while (this instanceof FastExecutionIndexingState &&
                (rollingIndex[2 * (depth - 1)] != iid)) {
            // Clear the top-of-stack
            stackOfCounters.get(depth).clear();

            // Decrement depth
            depth--;
        }
        // Of course, we still need to pop current
        // method.
        stackOfCounters.get(depth).clear();

        // Decrement depth
        depth--;
        assert (depth >= 0);
    }

    public ExecutionIndex getExecutionIndex(int iid) {
        // Increment counter for event (note: this is subject to hash collisions)
        int count = stackOfCounters.get(depth).increment(iid);

        // Add to rolling execution index
        rollingIndex[2*depth] = iid;
        rollingIndex[2*depth + 1] = count;

        // Snapshot the rolling index
        int size = 2*(depth+1); // 2 integers for each depth value
        int[] ei = Arrays.copyOf(rollingIndex, size);

        // Create an execution index
        return new ExecutionIndex(ei);
    }

    public ExecutionIndex getExecutionIndex() {
        // Snapshot the rolling index
        int size = 2*(depth); // 2 integers for each depth value
        int[] ei = Arrays.copyOf(rollingIndex, size);

        // Create an execution index
        return new ExecutionIndex(ei);
    }

}
