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
package edu.berkeley.cs.jqf.examples.kaitai;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.Size;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * @author Rohan Padhye
 */
public abstract class AbstractKaitaiGenerator extends Generator<InputStream> {

    public AbstractKaitaiGenerator() {
        super(InputStream.class);
    }

    private int capacity = Integer.MAX_VALUE;
    protected ByteBuffer buf;

    @SuppressWarnings("unused") // invoked by junit-quickcheck for @Size annotation
    public void configure(Size size) {
        this.capacity = size.max();
    }

    @Override
    public InputStream generate(SourceOfRandomness random, GenerationStatus status) {
        buf = ByteBuffer.allocate(this.capacity);
        try {
            // Populate byte buffer
            populate(random);

        } catch (BufferOverflowException e) {
            // throw new AssumptionViolatedException("Generated input is too large", e);
        }

        // Return the bytes as an inputstream
        int len = buf.position();
        buf.rewind();
        byte[] bytes = new byte[len];
        buf.get(bytes);
        return new ByteArrayInputStream(bytes);
    }


    abstract protected void populate(SourceOfRandomness random) throws BufferOverflowException;
}
