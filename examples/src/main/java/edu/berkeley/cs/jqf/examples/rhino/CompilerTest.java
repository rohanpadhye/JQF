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
package edu.berkeley.cs.jqf.examples.rhino;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.examples.common.AsciiStringGenerator;
import edu.berkeley.cs.jqf.examples.js.JavaScriptCodeGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Script;

@RunWith(JQF.class)
public class CompilerTest {

    private Context context;

    @Before
    public void initContext() {
        context = Context.enter();
    }

    @After
    public void exitContext() {
        context.exit();
    }

    @Fuzz
    public void testWithString(@From(AsciiStringGenerator.class) String input) {
        try {
            Script script = context.compileString(input, "input", 0, null);
        } catch (EvaluatorException e) {
            Assume.assumeNoException(e);
        }

    }

    @Fuzz
    public void debugWithString(@From(AsciiStringGenerator.class) String code) {
        System.out.println("\nInput:  " + code);
        testWithString(code);
        System.out.println("Success!");
    }

    @Test
    public void smallTest() {
        testWithString("x = 3 + 4");
        testWithString("x <<= undefined");
    }

    @Fuzz
    public void testWithInputStream(InputStream in) throws IOException {
        try {
            Script script = context.compileReader(new InputStreamReader(in), "input", 0, null);
        } catch (EvaluatorException e) {
            Assume.assumeNoException(e);
        }
    }

    @Fuzz
    public void debugWithInputStream(InputStream in) throws IOException {
        String input = IOUtils.toString(in, StandardCharsets.UTF_8);
        debugWithString(input);
    }

    @Fuzz
    public void testWithGenerator(@From(JavaScriptCodeGenerator.class) String code) {
        testWithString(code);
    }

    @Fuzz
    public void debugWithGenerator(@From(JavaScriptCodeGenerator.class) String code) {
        debugWithString(code);
    }



}
