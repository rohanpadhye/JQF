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

package edu.berkeley.cs.jqf.fuzz.guidance;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;


/**
 * This class extends {@link Random} to act as a generator of
 * "random" values, which themselves are read from a static file.
 *
 * The file-backed random number generator can be used for tuning the
 * "random" choices made by various <code>junit-quickcheck</code>
 * generators using a mutation-based genetic algorithm, in order to
 * maximize some objective function that can be measured from the
 * execution of each trial, such as code coverage.
 *
 *
 */
public class StreamBackedRandom extends Random {
    private final InputStream inputStream;
    private final ByteBuffer byteBuffer = ByteBuffer.allocate(4);
    private int totalBytesRead = 0;
    private int leadingBytesToIgnore = 0;

    /**
     * Constructs a stream-backed random generator.
     *
     * Also sets the seed of the underlying pseudo-random number
     * generator deterministically to zero.
     *
     * @param source  a generator of "random" bytes
     */
    public StreamBackedRandom(InputStream source) {
        super(0x5DEECE66DL);
        // Open the backing file source as a buffered input stream
        this.inputStream = source;
        // Force encoding to little-endian so that we can read small ints
        // by reading partially into the start of the buffer
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * Convenience constructor for use with the junit-quickcheck framework.
     *
     * The junit-quickcheck SourceOfRandomness annoyingly reads 8 bytes of
     * random data as soon as it is instantiated, only to configure its own
     * seed. As the seed is meaningless here, the 8 bytes get wasted. For
     * this purpose, this constructor allows specifying how many bytes of
     * requested random data to skip before starting to read from file.
     *
     * @param source  a generator of "random" bytes
     * @param leadinBytesToIgnore the number of leading bytes to ignore
     */
    public StreamBackedRandom(InputStream source, int leadinBytesToIgnore) {
        this(source);
        this.leadingBytesToIgnore = leadinBytesToIgnore;
    }

    /**
     * Generates upto 32 bits of random data for internal use by the Random
     * class.
     *
     * <p>Attempts to read up to 4 bytes of data from the input file, and
     * returns the requested lower order bits as a pseudo-random value.</p>
     *
     * <p>If end-of-file is reached before reading 4 bytes,
     * an {@link IllegalStateException} is thrown.</p>
     *
     * @param bits   the number of random bits to retain (1 to 32 inclusive)
     * @return the integer value whose lower <code>bits</code> bits contain the
     *    next random data available in the backing source
     * @throws IllegalStateException  if EOF has already been reached
     */
    @Override
    public int next(int bits) {
        // Ensure that up to 32 bits are being requested
        if (bits < 0 || bits > 32) {
            throw new IllegalArgumentException("Must read 1-32 bits at a time");
        }

        // Zero out the byte buffer before reading from the source
        byteBuffer.putInt(0, 0);

        try {
            // Read up to 4 bytes from the backing source
            int maxBytesToRead = ((bits + 7) / 8);
            assert(maxBytesToRead*8 >= bits && maxBytesToRead <= 4);

            if (this.leadingBytesToIgnore > 0) {
                int bytesToIgnore = Math.min(maxBytesToRead, this.leadingBytesToIgnore);
                this.leadingBytesToIgnore -= bytesToIgnore;
                maxBytesToRead -= bytesToIgnore;
            }

            // If fewer bytes are read (because EOF is reached), the buffer
            // just keeps containing zeros
            int actualBytesRead = inputStream.read(byteBuffer.array(), 0, maxBytesToRead);
            totalBytesRead += actualBytesRead;

            // If EOF was reached, throw an exception
            if (actualBytesRead != maxBytesToRead) {
                String message = String.format("EOF reached; total bytes read = %d, " +
                                "last read got %d of %d bytes",
                        totalBytesRead, actualBytesRead, maxBytesToRead);
                throw new IllegalStateException(new EOFException(message));

            }

        } catch (IOException e) {
            throw new GuidanceException(e);
        }

        // Interpret the bytes read as an integer
        int value = byteBuffer.getInt(0);

        // Return only the lower order bits as requested
        int mask = bits < 32 ? (1 << bits) - 1 : -1;
        return value & mask;

    }

    /**
     * Returns a pseudo-random value in the range <code>[0, bound)</code>,
     * consuming only as many whole bytes of the backing source as the bound
     * requires.
     *
     * <p>The original implementation always drew four bytes via
     * {@link #next(int) next(31)}. For a small bound such as a byte or boolean
     * range that wasted three of every four input bytes: the surplus bytes are
     * read and then discarded by the modulo, so mutating them cannot change the
     * result. Reading <code>ceil(log2(bound))</code> bits rounded up to whole
     * bytes keeps every consumed byte meaningful and shortens the input a
     * mutation-based fuzzer has to explore.</p>
     *
     * @param bound the exclusive upper bound; must be positive
     * @return a value in <code>[0, bound)</code>
     * @throws IllegalArgumentException if <code>bound</code> is not positive
     */
    @Override
    public int nextInt(int bound) {
        if (bound <= 0)
            throw new IllegalArgumentException("bound must be positive");
        // A bound of one always maps to zero, so no input needs to be consumed.
        if (bound == 1) {
            return 0;
        }
        // Number of significant bits in bound - 1, i.e. ceil(log2(bound)).
        int significantBits = 32 - Integer.numberOfLeadingZeros(bound - 1);
        if (significantBits <= 24) {
            // Round up to whole bytes: 1 byte for bound <= 256, 2 for <= 65536, 3 for <= 2^24.
            int bitsToRead = ((significantBits + 7) / 8) * 8;
            return next(bitsToRead) % bound;
        }
        // For very large bounds fall back to four bytes. next(31) stays
        // non-negative (unlike next(32)), so the modulo is well defined.
        return next(31) % bound;
    }

    public byte nextByte() {
        return (byte) next(Byte.SIZE);
    }

    public short nextShort() {
        return (short) next(Short.SIZE);
    }

    public int getTotalBytesRead() {
        return this.totalBytesRead;
    }

}
