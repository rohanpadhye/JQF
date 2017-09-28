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

import java.util.Random;

import com.pholser.junit.quickcheck.internal.Ranges;
import com.pholser.junit.quickcheck.internal.Ranges.Type;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * @author Rohan Padhye
 */
public class FastSourceOfRandomness extends SourceOfRandomness {

    private Random delegate;

    public FastSourceOfRandomness(Random delegate) {
        super(delegate);
        // Gotta make a copy of the reference because
        // super-class declares the field as private :-\
        this.delegate = delegate;
    }


    public int nextInt(int min, int max) {
        if (min == Integer.MIN_VALUE && max == Integer.MAX_VALUE) {
            return delegate.nextInt();
        }

        return this.fastChooseIntInRange(min, max);
    }

    public long nextLong(long min, long max) {
        int comparison = Ranges.checkRange(Type.INTEGRAL, min, max);

        if (min == Long.MIN_VALUE && max == Long.MAX_VALUE) {
            return delegate.nextLong();
        }

        return comparison == 0 ? min : Ranges.choose(this, min, max);
    }

    public short nextShort(short min, short max) {
        return (short)(this.fastChooseIntInRange(min, max));
    }

    public byte nextByte(byte min, byte max) {
        return (byte)(this.fastChooseIntInRange(min, max));
    }

    public char nextChar(char min, char max) {
        Ranges.checkRange(Type.CHARACTER, min, max);
        return (char)(this.fastChooseIntInRange(min, max));
    }

    private int fastChooseIntInRange(int min, int max) {
        int range = max - min;

        // If range is too wide, overflow will make it negative
        if (range > 0) {
            int random = delegate.nextInt() % range;
            if (random < 0) {
                random += range;
            }
            return min + random;
        } else {
            return (int) Ranges.choose(this, min, max);
        }
    }

}
