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
package edu.berkeley.cs.jqf.examples.errorprone;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.errorprone.ErrorProneCompiler;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.EqualsHashCode;
import com.pholser.junit.quickcheck.From;
import com.sun.tools.javac.main.Main;
import edu.berkeley.cs.jqf.examples.common.AsciiStringGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.sun.tools.javac.main.Main.Result.*;

/**
 * @author Rohan Padhye
 */
@RunWith(JQF.class)
public class CompilerTest {

    private Class<? extends BugChecker> bugCheckerClass = EqualsHashCode.class;
    private ErrorProneCompiler compiler;
    private final String[] args = {
            "-encoding", "UTF-8",
            "-XDdev",
            "-parameters",
            "-XDcompilePolicy=simple",
            "-proc:none"
    };


    private File writeToFile(String data) throws IOException {
        Path tmpDir = Files.createTempDirectory("errorprone-fuzz");
        Path javaFile = tmpDir.resolve("Test.java");

        try (BufferedWriter out = Files.newBufferedWriter(javaFile)) {
            out.write(data);
        }

        return javaFile.toFile();
    }

    @Fuzz
    public void testWithString(@From(AsciiStringGenerator.class) String classBody) {
        // Construct test class with provided body
        String code = String.format("class Test { %s }", classBody);
        File javaFile;
        try {
            javaFile = writeToFile(code);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Compile using javac + error-prone
        Main.Result result = ErrorProneCompiler.compile(new String[]{ javaFile.getAbsolutePath()},
                new PrintWriter(new ByteArrayOutputStream( )));

        // Ensure that there was no issue with test command or environment
        Assert.assertFalse(result == CMDERR);
        Assert.assertFalse(result == SYSERR);

        // Ignore compilation errors (only semantically valid inputs will pass)
        Assume.assumeFalse(result == ERROR);

        // This is the main test criteria -- abnormal exits are bugs
        Assert.assertFalse( result == ABNORMAL);

    }

    @Test
    public void test() {
        testWithString("{ System.out.println(0); }");
    }
}
