package edu.berkeley.cs.jqf.fuzz.util;

import org.eclipse.collections.api.iterator.IntIterator;
import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.map.mutable.primitive.IntIntHashMap;

/**
 * An implementation of  {@link Counter} that uses an IntIntHashMap to store values
 *
 * "Fast" in that it is faster than using something that involves other HashMaps,
 * and boxing to-and-from primitive values. There is surely a way to make it *faster*
 * than this too, without avoiding the collisions, but given the performance improvement
 * compared to the original (and collision-prone) coverage, I made the opinionated decision
 * to implement it this way to avoid other concerns from high collisions rates on some particular
 * target applications. Further experimentation with fast (non-colliding) coverage implementations
 * might help to determine which approach is preferable.
 *
 * @author Jonathan Bell
 */
public class FastNonCollidingCounter extends Counter {
    /** The counter map as a map of integers. */
    IntIntHashMap counts;

    /* List of indices in the map that are non-zero */
    protected IntArrayList nonZeroKeys;

    /**
     * Creates a new counter
     */
    public FastNonCollidingCounter(int size) {
        super(1);
        this.counts = new IntIntHashMap(size);
        this.nonZeroKeys = new IntArrayList(size / 2);
    }


    /**
     * Returns the size of this counter.
     *
     * @return the size of this counter
     */
    public synchronized int size() {
        return this.counts.size();
    }

    /**
     * Clears the counter by setting all values to zero.
     */
    public synchronized void clear() {
        this.counts.clear();
        this.nonZeroKeys.clear();
    }

    /**
     * Increments the count at the given key.
     *
     *
     * @param key the key whose count to increment
     * @return the new value after incrementing the count
     */
    public synchronized int increment(int key) {
        int newVal = this.counts.addToValue(key, 1);
        if (newVal == 1) {
            this.nonZeroKeys.add(key);
        }
        return newVal;
    }

    /**
     *
     * Increments the count at the given key by a given delta.
     *
     * @param key the key whose count to increment
     * @param delta the amount to increment by
     * @return the new value after incrementing the count
     */
    public synchronized int increment(int key, int delta) {
        int newVal = this.counts.addToValue(key, delta);
        if (newVal == delta) {
            nonZeroKeys.add(key);
        }
        return newVal;
    }

    @Override
    protected int incrementAtIndex(int index, int delta) {
        throw new UnsupportedOperationException("This coverage is already non-colliding, please just use get");
    }

    @Override
    public void setAtIndex(int idx, int value) {
        throw new UnsupportedOperationException("This coverage is already non-colliding, please just use setAtIndex");
    }

    @Override
    public int getAtIndex(int idx) {
        throw new UnsupportedOperationException("This coverage is already non-colliding, please just use set");
    }

    /**
     * Returns the number of indices with non-zero counts.
     *
     * @return the number of indices with non-zero counts
     */
    public synchronized int getNonZeroSize() {
        return nonZeroKeys.size();
    }


    /**
     * Returns a set of keys at which the count is non-zero.
     *
     * @return a set of keys at which the count is non-zero
     */
    public synchronized IntList getNonZeroKeys() {
        return this.nonZeroKeys;
    }

    public IntList getNonZeroIndices(){
        return this.getNonZeroKeys();
    }

    /**
     * Returns a set of non-zero count values in this counter.
     *
     * @return a set of non-zero count values in this counter.
     */
    public synchronized IntList getNonZeroValues() {
        IntArrayList values = new IntArrayList(this.counts.size() / 2);
        IntIterator iter = this.counts.values().intIterator();
        while (iter.hasNext()) {
            int val = iter.next();
            if (val != 0) {
                values.add(val);
            }
        }
        return values;
    }

    /**
     * Retreives a value for a given key.
     *
     * <p>The key is first hashed to retreive a value from
     * the counter, and hence the result is modulo collisions.</p>
     *
     * @param key the key to query
     * @return the count for the index corresponding to this key
     */
    public synchronized  int get(int key) {
        return this.counts.get(key);
    }

    public synchronized void copyFrom(FastNonCollidingCounter counter) {
        this.counts = new IntIntHashMap(counter.counts);
        this.nonZeroKeys = new IntArrayList(counter.nonZeroKeys.size());
        this.nonZeroKeys.addAll(counter.nonZeroKeys);
    }

    @Override
    public boolean hasNonZeros() {
        return !this.nonZeroKeys.isEmpty();
    }
}
