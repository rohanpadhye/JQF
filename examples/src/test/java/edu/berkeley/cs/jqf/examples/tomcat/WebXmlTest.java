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
package edu.berkeley.cs.jqf.examples.tomcat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.examples.xml.XMLDocumentUtils;
import edu.berkeley.cs.jqf.examples.xml.XmlDocumentGenerator;
import edu.berkeley.cs.jqf.examples.common.Dictionary;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.apache.tomcat.util.descriptor.web.WebXml;
import org.apache.tomcat.util.descriptor.web.WebXmlParser;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

@RunWith(JQF.class)
public class WebXmlTest {

    @Fuzz
    public void testWithInputStream(InputStream in) {
        InputSource inputSource = new InputSource(in);
        WebXml webXml = new WebXml();
        WebXmlParser parser = new WebXmlParser(false, false, true);
        boolean success = parser.parseWebXml(inputSource, webXml, false);
        Assume.assumeTrue(success);
    }

    @Fuzz
    public void testWithGenerator(@From(XmlDocumentGenerator.class) @Dictionary("dictionaries/tomcat-webxml.dict") Document dom) {
        testWithInputStream(XMLDocumentUtils.documentToInputStream(dom));
    }

    @Fuzz
    public void debugWithGenerator(@From(XmlDocumentGenerator.class) @Dictionary("dictionaries/tomcat-webxml.dict") Document dom) {
        System.out.println(XMLDocumentUtils.documentToString(dom));
        testWithGenerator(dom);
    }

    @Fuzz
    public void testWithString(String input){
        testWithInputStream(new ByteArrayInputStream(input.getBytes()));
    }

    @Test
    public void testSmall() {
        testWithString("<web-app xmlns=\"http://java.sun.com/xml/ns/javaee\" version=\"2.5\">\n" +
                "    <servlet>\n" +
                "        <servlet-name>comingsoon</servlet-name>\n" +
                "        <servlet-class>mysite.server.ComingSoonServlet</servlet-class>\n" +
                "    </servlet>\n" +
                "    <servlet-mapping>\n" +
                "        <servlet-name>comingsoon</servlet-name>\n" +
                "        <url-pattern>/*</url-pattern>\n" +
                "    </servlet-mapping>\n" +
                "</web-app>");
    }
}
