package edu.berkeley.cs.jqf.fuzz.mutation;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * ArraySet - an integer set backed by an array
 */
public class ArraySet {
    private static final int INITIAL_ARRAYSET_SIZE = 10;

    boolean[] bs = new boolean[INITIAL_ARRAYSET_SIZE];

    public void reset() {
        for (int i = 0; i < bs.length; i++)
            bs[i] = false;
    }

    public void add(int i) {
        ensureHas(i);
        bs[i] = true;
    }
    
    public boolean contains(int i) {
        return bs[i];
    }

    private void ensureHas(int i) {
        if (bs.length > i)
            return;
                
        int len = bs.length;

        while (len <= i)
            len *= 2;

        if (len != bs.length)
            bs = Arrays.copyOf(bs, len);
    }

    public int size() {
        int s = 0;
        for (int i = 0; i < bs.length; i++)
            if (bs[i])
                s++;
        return s;
    }
}
