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
package edu.berkeley.cs.jqf.examples.bcel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.verifier.StatelessVerifierFactory;
import org.apache.bcel.verifier.VerificationResult;
import org.apache.bcel.verifier.Verifier;
import org.junit.Assume;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

@RunWith(JQF.class)
public class ParserTest {

    @Fuzz
    public void testWithInputStream(InputStream inputStream) throws IOException {
        JavaClass clazz;
        try {
            clazz = new ClassParser(inputStream, "Hello.class").parse();
        } catch (ClassFormatException e) {
            // ClassFormatException thrown by the parser is just invalid input
            Assume.assumeNoException(e);
            return;
        }

        // Any non-IOException thrown here should be marked a failure
        // (including ClassFormatException)
        verifyJavaClass(clazz);
    }

    @Fuzz
    public void testWithGenerator(@From(JavaClassGenerator.class) JavaClass javaClass) throws IOException {

        try {
            // Dump the javaclass to a byte stream and get an input pipe
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            javaClass.dump(out);

            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            testWithInputStream(in);
        } catch (ClassFormatException e) {
            throw e;
        }
    }


    @Fuzz
    public void verifyJavaClass(@From(JavaClassGenerator.class) JavaClass javaClass) throws IOException {
        try {
            Repository.addClass(javaClass);
            Verifier verifier = StatelessVerifierFactory.getVerifier(javaClass.getClassName());
            VerificationResult result;
            result = verifier.doPass1();
            assumeThat(result.getMessage(), result.getStatus(), is(VerificationResult.VERIFIED_OK));
            result = verifier.doPass2();
            assumeThat(result.getMessage(), result.getStatus(), is(VerificationResult.VERIFIED_OK));
            for (int i = 0; i < javaClass.getMethods().length; i++) {
                result = verifier.doPass3a(i);
                assumeThat(result.getMessage(), result.getStatus(), is(VerificationResult.VERIFIED_OK));
            }
        } finally {
            Repository.clearCache();
        }
    }

}
