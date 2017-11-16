/*
 * Copyright (c) 2017, University of California, Berkeley
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
package edu.berkeley.cs.jqf.examples.jdk;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.opensymphony.xwork2.validator.validators.URLValidator;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.generator.Size;
import edu.berkeley.cs.jqf.examples.common.AsciiStringGenerator;
import edu.berkeley.cs.jqf.fuzz.junit.Fuzz;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.JQF;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Rohan Padhye
 */
@RunWith(JQF.class)
public class RegexTest {

    private static final int INPUT_SIZE = 40;

    @Fuzz
    public void patternTest(@From(AsciiStringGenerator.class) String pattern) {
        try {
            Pattern.matches(pattern, "aaaaaa");
        } catch (PatternSyntaxException e) {
            Assume.assumeNoException(e);
        }
    }

    @Fuzz
    public void matchTest(@From(AsciiStringGenerator.class) String input) {
        Pattern.matches("^(([a-z])+.)+[A-Z]([a-z])+$", input);
    }

    @Test
    public void exploitPatternTest() {
        String pattern = ".*(P*.*()*(.*.*())*.*())*\\R*.*b$";
        patternTest(pattern);

    }

    private static Pattern strutsPattern;

    @BeforeClass
    public static void initStrutsPattern() {
        URLValidator validator = new URLValidator();
        strutsPattern = Pattern.compile(validator.getUrlRegex(), Pattern.CASE_INSENSITIVE);
    }


    @Fuzz
    public void strutsTest(@From(AsciiStringGenerator.class) @Size(max=80) String url) {
        strutsPattern.matcher(url).matches();
    }

    @Test
    public void exploitStrutsTest() {
        String url = "ftp://aaaaaaaaaaaaaaaaaaaaaaaa|";
        strutsTest(url);
    }
}
