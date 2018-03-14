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
package edu.berkeley.cs.jqf.fuzz.util;

/**
 * Utility class for computing bounded hash values.
 *
 * @author Rohan Padhye
 */
public class Hashing {

    private Hashing() {
        // Static only
    }

    private static int cap(long x, int bound) {
        int res = (int) (x % bound);
        if (res < 0) {
            res += bound;
        }
        return res;
    }

    /**
     * Compute knuth's multiplicative hash.
     *
     * <p>Source: Donald Knuth's <em>The Art of Computer Programming</em>,
     * Volume 3 (2nd edition), section 6.4, page 516.</p>
     *
     * @param x     the input value to hash
     * @param bound the upper bound
     * @return the hash value
     */
    protected static int knuth(long x, int bound) {
        return cap(x*2654435761L, bound);
    }

    /**
     * Returns a bounded hashed value with one input.
     *
     * @param x     the input to hash
     * @param bound the upper bound
     * @return a pseudo-uniformly distributed value in [0, bound)
     */
    public static int hash(long x, int bound) {
        return knuth(x, bound);
    }

    /**
     * Returns a bounded hashed value with two inputs.
     *
     * @param x     the first input to hash
     * @param y     the second input to hash
     * @param bound the upper bound
     * @return a pseudo-uniformly distributed value in [0, bound)
     */
    public static int hash1(long x, long y, int bound) {
        return knuth(x*31 + y, bound);
    }

}
