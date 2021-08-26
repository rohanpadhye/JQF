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

import chess.Situation;
import chess.format.Forsyth;
import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.examples.common.ArbitraryLengthStringGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import scala.Option;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

/**
 * @author Rohan Padhye
 */
@RunWith(JQF.class)
public class FENTest {

    public static final String INITIAL_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    @Test
    public void test() {
        testWithString(INITIAL_FEN);
    }

    @Test
    public void debug() {
        debugWithString(INITIAL_FEN);
        debugWithString("Q");
    }

    private Situation parseFEN(String fen) {
        Option<Situation> situation = Forsyth.$less$less(fen);
        Assume.assumeTrue(situation.isDefined());
        return situation.get();
    }

    @Fuzz
    public void testWithString(@From(ArbitraryLengthStringGenerator.class) String fen) {
        Situation situation = parseFEN(fen);
        assumeTrue(situation.playable(true));
        assertThat(situation.moves().size(), greaterThan(0));
    }

    @Fuzz
    public void debugWithString(@From(ArbitraryLengthStringGenerator.class) String fen) {
        Situation situation = parseFEN(fen);
        System.out.println(situation.moves().values());
    }

    @Fuzz
    public void testWithGenerator(@From(FENGenerator.class) String fen) {
        testWithString(fen);
    }

    @Fuzz
    public void debugWithGenerator(@From(FENGenerator.class) String fen) {
        System.out.println(fen);//debugWithString(fen);
    }

    @Fuzz
    public void testWithInputStream(@From(ArbitraryLengthStringGenerator.class) String fen) {
        testWithString(fen);
    }
}
