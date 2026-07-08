/*
 * Copyright (c) 2026 Vladimir Sitnikov and JQF Contributors
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

import java.io.ByteArrayInputStream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link StreamBackedRandom#nextInt(int)}, which draws only as many
 * whole bytes of the backing source as the bound requires.
 */
public class StreamBackedRandomTest {

    /** Builds a generator backed by the given bytes (each argument is one unsigned byte). */
    private static StreamBackedRandom backedBy(int... unsignedBytes) {
        byte[] bytes = new byte[unsignedBytes.length];
        for (int i = 0; i < unsignedBytes.length; i++) {
            bytes[i] = (byte) unsignedBytes[i];
        }
        return new StreamBackedRandom(new ByteArrayInputStream(bytes));
    }

    @Test
    public void boundOfOneConsumesNoBytes() {
        // No bytes are available, yet a bound of one must still succeed.
        StreamBackedRandom random = backedBy();
        assertEquals(0, random.nextInt(1));
        assertEquals(0, random.getTotalBytesRead());
    }

    @Test
    public void byteRangeConsumesOneByteEach() {
        // bound = 256 reads a single byte and returns its unsigned value.
        StreamBackedRandom random = backedBy(0x00, 0x7F, 0x80, 0xFF);
        assertEquals(0x00, random.nextInt(256));
        assertEquals(0x7F, random.nextInt(256));
        assertEquals(0x80, random.nextInt(256));
        assertEquals(0xFF, random.nextInt(256));
        assertEquals(4, random.getTotalBytesRead());
    }

    @Test
    public void twoByteBoundReadsTwoBytesLittleEndian() {
        // bound = 2^16 reads exactly two bytes in little-endian order.
        StreamBackedRandom random = backedBy(0x01, 0x02);
        assertEquals(0x0201, random.nextInt(1 << 16));
        assertEquals(2, random.getTotalBytesRead());
    }

    @Test
    public void threeByteBoundReadsThreeBytesLittleEndian() {
        // bound = 2^24 reads exactly three bytes in little-endian order.
        StreamBackedRandom random = backedBy(0x01, 0x02, 0x03);
        assertEquals(0x030201, random.nextInt(1 << 24));
        assertEquals(3, random.getTotalBytesRead());
    }

    @Test
    public void consumesMinimalBytesPerBound() {
        assertBytesConsumed(1, 0);
        assertBytesConsumed(2, 1);
        assertBytesConsumed(256, 1);
        assertBytesConsumed(257, 2);
        assertBytesConsumed(1 << 16, 2);
        assertBytesConsumed((1 << 16) + 1, 3);
        assertBytesConsumed(1 << 24, 3);
        assertBytesConsumed((1 << 24) + 1, 4);
        assertBytesConsumed(1 << 30, 4);
        assertBytesConsumed(Integer.MAX_VALUE, 4);
    }

    @Test
    public void singleByteBoundStaysInRangeForEveryByteValue() {
        // bound = 100 needs seven bits, so one byte is drawn and reduced modulo 100.
        for (int b = 0; b < 256; b++) {
            StreamBackedRandom random = backedBy(b);
            int value = random.nextInt(100);
            assertTrue("value out of range for byte " + b, value >= 0 && value < 100);
            assertEquals(b % 100, value);
            assertEquals(1, random.getTotalBytesRead());
        }
    }

    @Test
    public void largeBoundStaysNonNegative() {
        // All-ones input makes next(31) return 0x7FFFFFFF, which stays non-negative.
        StreamBackedRandom random = backedBy(0xFF, 0xFF, 0xFF, 0xFF);
        int value = random.nextInt(Integer.MAX_VALUE);
        assertTrue("value must be non-negative", value >= 0);
        assertEquals(4, random.getTotalBytesRead());
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroBoundThrows() {
        backedBy(1, 2, 3, 4).nextInt(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeBoundThrows() {
        backedBy(1, 2, 3, 4).nextInt(-5);
    }

    /** Draws one value for {@code bound} and asserts the byte count and range. */
    private static void assertBytesConsumed(int bound, int expectedBytes) {
        StreamBackedRandom random = new StreamBackedRandom(new ByteArrayInputStream(new byte[8]));
        int value = random.nextInt(bound);
        assertTrue("value out of range for bound " + bound, value >= 0 && value < bound);
        assertEquals("bytes consumed for bound " + bound, expectedBytes, random.getTotalBytesRead());
    }
}
