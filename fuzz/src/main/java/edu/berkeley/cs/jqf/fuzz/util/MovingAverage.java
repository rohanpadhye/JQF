package edu.berkeley.cs.jqf.fuzz.util;

import java.util.Arrays;

/**
 * MovingAverage - A data structure for collecting a moving average.
 *
 * @author Raffi Sanna
 */
public class MovingAverage {
    /** The most recently seen values */
    int recent[];

    /** The amount of elements filled with valid values */
    int size = 0;

    /** The next slot to be filled in */
    int next = 0;

    /**
     * Constructs a moving average of the most recent {@code cap} arguments
     * 
     * @param cap The number of elements to be remembered
     */
    public MovingAverage(int cap) {
        assert cap > 0;
        recent = new int[cap];
    }

    /**  
     * Adds a number to the set of numbers used to find the moving average.
     *
     * @param a The number to be added
     */
    public void add(int a) {
        size = Integer.min(recent.length, size + 1);
        recent[next] = a;
        next = (next + 1) % recent.length;
    }

    /** 
     * Gets the moving average itself, or {@code 0} if nothing has been added.
     * 
     * @return the moving average
     */
    public double get() {
        return Arrays.stream(recent).limit(size).average().orElse(0);
    }
}
