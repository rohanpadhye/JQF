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
package edu.berkeley.cs.jqf.fuzz.junit.quickcheck;

import java.util.Random;

import com.pholser.junit.quickcheck.internal.Ranges;
import com.pholser.junit.quickcheck.internal.Ranges.Type;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;

/**
 * A source of randomness with better performance but looser
 * statistical guarantees.
 *
 * This class is meant for use with guided fuzzing, where the
 * {@link Random} delegate is usually a {@link StreamBackedRandom}.
 * In this case, the random source does not have to give any
 * statistical guarantees such as uniformity or independentness,
 * and therefore is amenable to several optimizations, which are
 * implemented in this class.
 *
 * @author Rohan Padhye
 */
public class FastSourceOfRandomness extends SourceOfRandomness {

    private StreamBackedRandom delegate;

    public FastSourceOfRandomness(StreamBackedRandom delegate) {
        super(delegate);
        // Gotta make a copy of the reference because
        // super-class declares the field as private :-\
        this.delegate = delegate;
    }

    @Override
    public Random toJDKRandom() {
        return this.delegate;
    }

    @Override
    public byte nextByte(byte min, byte max) {
        if (min == Byte.MIN_VALUE && max == Byte.MAX_VALUE) {
            return delegate.nextByte();
        } else if (min >= Byte.MIN_VALUE && max <= Byte.MAX_VALUE) {

        }
        return this.fastChooseByteInRange(min, max);
    }

    @Override
    public short nextShort(short min, short max) {
        if (min == Short.MIN_VALUE && max == Short.MAX_VALUE) {
            return delegate.nextShort();
        }
        return (short)(this.fastChooseIntInRange(min, max));
    }

    @Override
    public char nextChar(char min, char max) {
        Ranges.checkRange(Type.CHARACTER, min, max);
        return (char)(this.fastChooseIntInRange(min, max));

    }

    @Override
    public int nextInt(int min, int max) {
        if (min == Integer.MIN_VALUE && max == Integer.MAX_VALUE) {
            return delegate.nextInt();
        }

        return this.fastChooseIntInRange(min, max);
    }

    @Override
    public long nextLong(long min, long max) {
        int comparison = Ranges.checkRange(Type.INTEGRAL, min, max);

        if (min == Long.MIN_VALUE && max == Long.MAX_VALUE) {
            return delegate.nextLong();
        }

        return comparison == 0 ? min : Ranges.choose(this, min, max);
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

    private byte fastChooseByteInRange(byte min, byte max) {
        int range = max - min;

        // If range is too wide, overflow will make it negative
        if (range > 0 && range <= (Byte.MAX_VALUE)) {
            int random = delegate.nextByte() % range;
            if (random < 0) {
                random += range;
            }
            byte result = (byte) (min + random);
            assert (result >= min && result <= max);
            return result;
        } else {
            return (byte) fastChooseIntInRange((int) min, (int) max);
        }
    }

}
