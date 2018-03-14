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
package edu.berkeley.cs.jqf.fuzz.ei;

import java.util.Arrays;

/**
 * An execution index represents a unique point in a program's execution.
 *
 * <p>This class uses a call-stack-with-counts representation of execution
 * indexes, which was first introduced in the paper <em>A randomized dynamic
 * program analysis technique for detecting real deadlocks</em> by
 * Joshi et al. in PLDI 2009.</p>
 *
 * <p>The execution index is a basically a wrapper around an integer array
 * of even length, in which every pair of elements represents an IID of a
 * call site and its associated count.</p>
 *
 * @author Rohan Padhye
 */
public class ExecutionIndex implements Comparable<ExecutionIndex> {

    final int[] ei;

    public ExecutionIndex(int[] ei) {
        if (ei.length == 0 || ei.length % 2 == 1) {
            throw new IllegalArgumentException("Execution index must have non-zero even elements");
        }
        this.ei = ei;
    }

    public ExecutionIndex(Prefix prefix, Suffix suffix) {
        // Prefix must end where suffix begins
        if (prefix.length != suffix.offset) {
            throw new IllegalArgumentException("Invalid prefix/suffix combo");
        }

        // The size of this EI is the same as the size of the owner of suffix
        int size = suffix.ei.ei.length;
        this.ei = new int[size];
        int[] prefixEi = prefix.ei.ei;
        for (int i = 0; i < prefix.length; i++) {
            this.ei[i] = prefixEi[i];
        }
        int[] suffixEi = suffix.ei.ei;
        for (int i = suffix.offset; i < size; i++) {
            this.ei[i] = suffixEi[i];
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(ei);
    }

    @Override
    public boolean equals(Object other) {
        if (other != null && other instanceof ExecutionIndex) {
            return Arrays.equals(ei, ((ExecutionIndex) other).ei);
        } else {
            return false;
        }
    }

    @Override
    public int compareTo(ExecutionIndex other) {
        int len1 = ei.length;
        int len2 = other.ei.length;
        int lim = Math.min(len1, len2);
        int v1[] = ei;
        int v2[] = other.ei;

        int k = 0;
        while (k < lim) {
            int c1 = v1[k];
            int c2 = v2[k];
            if (c1 != c2) {
                return c1 - c2;
            }
            k++;
        }
        return len1 - len2;
    }

    @Override
    public String toString() {
        return Arrays.toString(ei);
    }

    public int oneSuffixSize() {
        int size = 0;
        for (int i = ei.length-1; i >= 0; i -= 2) {
            if (ei[i] == 1) {
                size++;
            }
        }
        return size;
    }

    public Suffix getCommonSuffix(ExecutionIndex other) {
        // Do an inexpensive check of ExecutionContext(this) == ExecutionContext(other)
        if (this.ei.length != other.ei.length) {
            throw new IllegalArgumentException("Common suffix can only be computed on " +
                    "execution indexes with same execution contexts");
        }

        int offset = ei.length;
        while (offset > 0) {
            if (this.ei[offset-2] == other.ei[offset-2] && this.ei[offset-1] == other.ei[offset-1]) {
                offset -= 2;
            } else {
                break;
            }
        }
        return new Suffix(this, offset);

    }

    public Prefix getPrefixOfSuffix(Suffix suffix) {
        // prefix length = suffix offset
        return new Prefix(this, suffix.offset);
    }

    public Suffix getSuffixOfPrefix(Prefix prefix) {
        // suffix offset = prefix length
        return new Suffix(this, prefix.length);
    }

    public boolean hasPrefix(Prefix prefix) {
        int[] cmpEi = prefix.ei.ei;
        for (int i = 0; i < prefix.length; i++) {
            if (ei[i] != cmpEi[i]) {
                return false;
            }
        }
        return true;
    }

    public static class Prefix {
        private final ExecutionIndex ei;
        private final int length;

        public Prefix(ExecutionIndex ei, int length) {
            this.ei = ei;
            this.length = length;
        }

        public int size() {
            return length/2;
        }

        public ExecutionIndex getEi() {
            return ei;
        }
    }

    public static class Suffix {
        private final ExecutionIndex ei;
        private final int offset;

        public Suffix(ExecutionIndex ei, int offset) {
            this.ei = ei;
            this.offset = offset;
        }

        public int size() {
            return (ei.ei.length - offset)/2;
        }

        public ExecutionIndex getEi() {
            return ei;
        }
    }

}
