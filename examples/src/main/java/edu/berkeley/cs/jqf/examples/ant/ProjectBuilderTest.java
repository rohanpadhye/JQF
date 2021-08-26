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
package edu.berkeley.cs.jqf.examples.ant;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.examples.xml.XMLDocumentUtils;
import edu.berkeley.cs.jqf.examples.xml.XmlDocumentGenerator;
import edu.berkeley.cs.jqf.examples.common.Dictionary;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.helper.ProjectHelperImpl;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;

@RunWith(JQF.class)
public class ProjectBuilderTest {

    private File serializeInputStream(InputStream in) throws IOException {
        Path path = Files.createTempFile("build", ".xml");
        try (BufferedWriter out = Files.newBufferedWriter(path)) {
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
        }
        return path.toFile();
    }

    @Fuzz
    public void testWithInputStream(InputStream in) {
        File buildXml = null;
        try {
            buildXml = serializeInputStream(in);
            ProjectHelperImpl p = new ProjectHelperImpl();
            p.parse(new Project(), buildXml);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (BuildException e) {
            Assume.assumeNoException(e);
        } finally {
            if (buildXml != null) {
                buildXml.delete();
            }
        }
    }

    @Fuzz
    public void testWithGenerator(@From(XmlDocumentGenerator.class)
                                      @Dictionary("dictionaries/ant-project.dict") Document dom) {
        testWithInputStream(XMLDocumentUtils.documentToInputStream(dom));
    }

    @Fuzz
    public void debugWithGenerator(@From(XmlDocumentGenerator.class)
                                       @Dictionary("dictionaries/ant-project.dict") Document dom) {
        System.out.println(XMLDocumentUtils.documentToString(dom));
        testWithGenerator(dom);
    }

    @Fuzz
    public void testWithString(String input) throws BuildException {
        testWithInputStream(new ByteArrayInputStream(input.getBytes()));
    }

    @Test
    public void testSmall() throws BuildException {
        testWithString("<project default='s' />");
    }
}
