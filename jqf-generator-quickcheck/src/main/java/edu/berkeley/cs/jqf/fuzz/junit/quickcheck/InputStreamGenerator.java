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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * Lazy provider of bytes from an input stream. This is useful
 * when you want the parameter sequence to be exactly equal to
 * the raw input, such as when fuzzing with AFL.
 *
 * Use this only with AFL-like front-ends, which return
 * bytes from a fixed-size file. This generator may not work
 * properly with front-ends like Zest that generate fresh
 * values at the end of a parameter sequence, since that would
 * make the InputStream infinitely long.
 *
 * @author Rohan Padhye
 */
public class InputStreamGenerator extends Generator<InputStream> {
    public InputStreamGenerator() {
        super(InputStream.class);
    }

    @Override
    public InputStream generate(SourceOfRandomness sourceOfRandomness, GenerationStatus generationStatus) {
        return new InputStream() {
           @Override
           public int read() throws IOException {
               // Keep asking for new random bytes until the
               // SourceOfRandomness runs out of parameters. This is designed
               // to work with fixed-size parameter sequences, such as when
               // fuzzing with AFL.
               try {
                   byte nextByte = sourceOfRandomness.nextByte(Byte.MIN_VALUE, Byte.MAX_VALUE);
                   int nextInt = nextByte & 0xFF;
                   return nextInt;
               } catch (IllegalStateException e) {
                   if (e.getCause() instanceof EOFException) {
                       return -1;
                   } else {
                       throw e;
                   }
               }
           }
       };
    }
}
