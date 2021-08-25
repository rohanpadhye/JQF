/*
 * Copyright (c) 2018, The Regents of the University of California
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
package edu.berkeley.cs.jqf.examples.chess;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * @author Rohan Padhye
 */
public class FENGenerator extends Generator<String> {

    public FENGenerator() {
        super(String.class);
    }

    @Override
    public String generate(SourceOfRandomness r, GenerationStatus g) {
        return String.join(" ", generateBoard(r), generateColor(r), generateCastles(r),
                generateEnPassant(r), generateHalfMoveClock(r), generateFullMoveClock(r));
    }

    private char[] pieces = { 'K', 'Q', 'R', 'B', 'N', 'P', 'k', 'q', 'r', 'b', 'n', 'p'};

    private String generateBoard(SourceOfRandomness r) {
        String[] rows = new String[8];
        for (int i = 0; i < 8; i++) {
            String row = "";
            for (int j = 0; j < 8; j++) {
                if (r.nextBoolean()) {
                    // empty square
                    int skip = r.nextInt(0, 8-j);
                    j += skip;
                    row += String.valueOf(skip+1); // Upper bound is exclusive
                } else {
                    // piece
                    row += pieces[r.nextInt(pieces.length)];
                }
            }
            rows[i] = row;
        }
        return String.join("/", rows);
    }

    private String generateColor(SourceOfRandomness r) {
        return r.nextBoolean() ? "w" : "b";
    }

    private String generateCastles(SourceOfRandomness r) {
        if (r.nextBoolean()) {
            return "-";
        }
        String castle = "";
        if (r.nextBoolean()) {
            castle += "K";
        }
        if (r.nextBoolean()) {
            castle += "Q";
        }
        if (r.nextBoolean()) {
            castle += "k";
        }
        if (r.nextBoolean()) {
            castle += "q";
        }
        if (castle.isEmpty()) {
            castle = "-";
        }
        return castle;
    }

    private String generateEnPassant(SourceOfRandomness r) {
        if (r.nextBoolean()) {
            return "-";
        }
        char x = r.nextChar('a', 'i'); // Upper-bound is exclusive
        int y =  r.nextInt(1, 9);      // Upper-bound is exclusive
        return String.valueOf(x) + String.valueOf(y);
    }

    private String generateHalfMoveClock(SourceOfRandomness r) {
        return Integer.toString(r.nextInt(0, 50));
    }

    private String generateFullMoveClock(SourceOfRandomness r) {
        return Integer.toString(r.nextInt(1, 100));
    }
}
