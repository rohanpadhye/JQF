package edu.berkeley.cs.jqf.fuzz.util;

import org.eclipse.collections.api.list.primitive.IntList;

public interface ICoverage<T extends Counter> {
    /**
     * Returns the size of the coverage map.
     *
     * @return the size of the coverage map
     */
    int size();

    /**
     * Returns the number of edges covered.
     *
     * @return the number of edges with non-zero counts
     */
    int getNonZeroCount();

    /**
     * Returns a collection of branches that are covered.
     *
     * @return a collection of keys that are covered
     */
    IntList getCovered();

    /**
     * Returns a set of edges in this coverage that don't exist in baseline
     *
     * @param baseline the baseline coverage
     * @return the set of edges that do not exist in {@code baseline}
     */
    IntList computeNewCoverage(ICoverage baseline);

    /**
     * Clears the coverage map.
     */
    void clear();

    /**
     * Updates this coverage with bits from the parameter.
     *
     * @param that the run coverage whose bits to OR
     *
     * @return <code>true</code> iff <code>that</code> is not a subset
     *         of <code>this</code>, causing <code>this</code> to change.
     */
    boolean updateBits(ICoverage that);

    /**
     * Returns a hash code of the list of edges that have been covered at least once.
     *
     * @return a hash of non-zero entries
     */
    int nonZeroHashCode();

    Counter getCounter();

    ICoverage<T> copy();
}
