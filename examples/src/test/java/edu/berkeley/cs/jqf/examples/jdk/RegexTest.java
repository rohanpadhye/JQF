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
package edu.berkeley.cs.jqf.examples.jdk;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.opensymphony.xwork2.validator.validators.EmailValidator;
import com.opensymphony.xwork2.validator.validators.URLValidator;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.generator.Size;
import edu.berkeley.cs.jqf.examples.common.ArbitraryLengthStringGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.Assume;
import org.junit.runner.RunWith;

/**
 * @author Rohan Padhye
 */
@RunWith(JQF.class)
public class RegexTest {

    @Fuzz
    public void patternGenerationTest(@From(ArbitraryLengthStringGenerator.class) String pattern) {
        try {
            Pattern.matches(pattern, "aaaaaa");
        } catch (PatternSyntaxException e) {
            Assume.assumeNoException(e);
        }
    }

    @Fuzz
    public void exponentialMatchTest(@From(ArbitraryLengthStringGenerator.class) String input) {
        Pattern.matches("^(([a-z])+.)+[A-Z]([a-z])+$", input);
    }

    /* Regexes from Apache Struts */

    private static final Pattern strutsUrlPattern = Pattern.compile(new URLValidator().getUrlRegex(), Pattern.CASE_INSENSITIVE);
    private static final Pattern strutsEmailPattern = Pattern.compile(new EmailValidator().getRegex(), Pattern.CASE_INSENSITIVE);


    @Fuzz
    public void strutsUrlTest(@From(ArbitraryLengthStringGenerator.class) @Size(max=80) String url) {
        strutsUrlPattern.matcher(url).matches();
    }

    @Fuzz
    public void strutsEmailTest(@From(ArbitraryLengthStringGenerator.class) String email) {
        strutsEmailPattern.matcher(email).matches();
    }
}
