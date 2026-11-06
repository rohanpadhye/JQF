/*
 * Copyright (c) 2026 François Martin
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

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class FastSourceOfRandomnessTest {

    private static FastSourceOfRandomness backedBy(int... words) {
        // SourceOfRandomness consumes eight bytes to initialize its unused seed.
        ByteBuffer bytes = ByteBuffer.allocate(8 + words.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        bytes.position(8);
        for (int word : words) {
            bytes.putInt(word);
        }
        return new FastSourceOfRandomness(new StreamBackedRandom(new ByteArrayInputStream(bytes.array())));
    }

    @Test
    public void integerRangeIncludesBothEndpoints() {
        assertEquals(10, backedBy(0).nextInt(10, 11));
        assertEquals(11, backedBy(1).nextInt(10, 11));
        assertEquals(15, backedBy(5).nextInt(10, 15));
    }

    @Test
    public void negativeAndCrossZeroRangesIncludeUpperEndpoint() {
        assertEquals(-1, backedBy(4).nextInt(-5, -1));
        assertEquals(2, backedBy(4).nextInt(-2, 2));
        assertEquals(2, backedBy(-1).nextInt(-2, 2));
    }

    @Test
    public void narrowRangesAtIntegerLimitsIncludeUpperEndpoint() {
        assertEquals(Integer.MAX_VALUE, backedBy(1).nextInt(Integer.MAX_VALUE - 1, Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE + 1, backedBy(1).nextInt(Integer.MIN_VALUE, Integer.MIN_VALUE + 1));
    }

    @Test
    public void overflowingWidthIncludesBothEndpoints() {
        assertEquals(0, backedBy(0, 0).nextInt(0, Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, backedBy(0, Integer.MAX_VALUE).nextInt(0, Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, backedBy(0, 0).nextInt(Integer.MIN_VALUE, 0));
        assertEquals(0, backedBy(1, Integer.MIN_VALUE).nextInt(Integer.MIN_VALUE, 0));
        assertEquals(Integer.MAX_VALUE, backedBy(1, -2).nextInt(Integer.MIN_VALUE + 1, Integer.MAX_VALUE));
    }

    @Test
    public void fullIntegerRangePreservesRawValues() {
        assertEquals(Integer.MIN_VALUE, backedBy(Integer.MIN_VALUE).nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, backedBy(Integer.MAX_VALUE).nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE));
    }

    @Test
    public void singletonRangesConsumeNoInput() {
        assertEquals(42, backedBy().nextInt(42, 42));
        assertEquals(Integer.MAX_VALUE, backedBy().nextInt(Integer.MAX_VALUE, Integer.MAX_VALUE));
        assertEquals(Integer.MIN_VALUE, backedBy().nextInt(Integer.MIN_VALUE, Integer.MIN_VALUE));
        assertEquals(42, backedBy().nextByte((byte) 42, (byte) 42));
        assertEquals(42, backedBy().nextShort((short) 42, (short) 42));
        assertEquals('x', backedBy().nextChar('x', 'x'));
    }

    @Test
    public void reversedRangesThrowBeforeReadingInput() {
        assertThrows(IllegalArgumentException.class, () -> backedBy().nextInt(1, 0));
        assertThrows(IllegalArgumentException.class, () -> backedBy().nextInt(Integer.MAX_VALUE, Integer.MIN_VALUE));
        assertThrows(IllegalArgumentException.class, () -> backedBy().nextByte((byte) 1, (byte) 0));
        assertThrows(IllegalArgumentException.class, () -> backedBy().nextShort((short) 1, (short) 0));
        assertThrows(IllegalArgumentException.class, () -> backedBy().nextChar('z', 'a'));
    }

    @Test
    public void shortAndCharacterRangesIncludeUpperEndpoint() {
        assertEquals(11, backedBy(1).nextShort((short) 10, (short) 11));
        assertEquals(Short.MAX_VALUE, backedBy(1).nextShort((short) (Short.MAX_VALUE - 1), Short.MAX_VALUE));
        assertEquals('z', backedBy(25).nextChar('a', 'z'));
        assertEquals(Character.MAX_VALUE, backedBy(65535).nextChar(Character.MIN_VALUE, Character.MAX_VALUE));
    }

    @Test
    public void byteFastAndFallbackRangesIncludeUpperEndpoint() {
        assertEquals(11, backedBy(1).nextByte((byte) 10, (byte) 11));
        assertEquals(126, backedBy(126).nextByte((byte) 0, (byte) 126));
        assertEquals(127, backedBy(127).nextByte((byte) 0, (byte) 127));
        assertEquals(127, backedBy(128).nextByte((byte) -1, (byte) 127));
        assertEquals(-1, backedBy(-1).nextByte((byte) -5, (byte) -1));
    }

    @Test
    public void byteRangeStaysWithinBoundsForEveryInputByte() {
        boolean[] seen = new boolean[5];
        for (int input = 0; input < 256; input++) {
            byte value = backedBy(input).nextByte((byte) -2, (byte) 2);
            assertTrue(value >= -2 && value <= 2);
            seen[value + 2] = true;
        }
        for (boolean present : seen) {
            assertTrue(present);
        }
    }

    @Test
    public void fullByteAndShortRangesPreserveRawValues() {
        assertEquals(Byte.MIN_VALUE, backedBy(128).nextByte(Byte.MIN_VALUE, Byte.MAX_VALUE));
        assertEquals(Byte.MAX_VALUE, backedBy(127).nextByte(Byte.MIN_VALUE, Byte.MAX_VALUE));
        assertEquals(Short.MIN_VALUE, backedBy(32768).nextShort(Short.MIN_VALUE, Short.MAX_VALUE));
        assertEquals(Short.MAX_VALUE, backedBy(32767).nextShort(Short.MIN_VALUE, Short.MAX_VALUE));
    }
}
