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
package edu.berkeley.cs.jqf.examples.imageio;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import edu.berkeley.cs.jqf.fuzz.junit.Fuzz;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.JQF;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JQF.class)
public class PngReaderTest {

    @BeforeClass
    public static void disableCaching() {
        // Disable disk-caching as it slows down fuzzing
        // and makes image reads non-idempotent
        ImageIO.setUseCache(false);
    }

    private ImageReader reader;

    @Before
    public void setUp() {
        this.reader = ImageIO.getImageReadersByFormatName("png").next();
    }

    @After
    public void tearDown() {
        this.reader.dispose();
    }

    @Fuzz
    public void read(ImageInputStream input) {
        try {
            // Decode image from input stream
            ImageReadParam param = reader.getDefaultReadParam();
            reader.setInput(input);
            reader.read(0);
        } catch (IOException e) {
            // Ignore decode errors
        }

    }

    @Fuzz
    public void getWidth(ImageInputStream input) {
        try {
            // Decode image from input stream
            reader.setInput(input);
            int width = reader.getWidth(0);
            System.out.println(width);
        } catch (IOException e) {
            System.err.println("Bad image: " + e.getMessage());
        }
    }

    @Fuzz
    public void getHeight(ImageInputStream input) {
        try {
            // Decode image from input stream
            reader.setInput(input);
            int height = reader.getHeight(0);
            System.out.println(height);
        } catch (IOException e) {
            System.err.println("Bad image: " + e.getMessage());
        }
    }


    @Fuzz
    public void getMetaData(ImageInputStream input) {
        try {
            // Decode image from input stream
            reader.setInput(input);
            reader.getImageMetadata(0);
        } catch (IOException e) {
            // Ignore decode errors
        }
    }

    @Fuzz
    public void readBounded(ImageInputStream input) {
        try {
            // Decode image from input stream
            reader.setInput(input);
            int width = reader.getWidth(0);
            int height = reader.getHeight(0);
            Assume.assumeTrue(width > 0 && width < 100);
            Assume.assumeTrue(height > 0 && height < 100);
            reader.read(0);
        } catch (IOException e) {
            // Ignore decode errors
        }
    }

    @Test
    public void debug() throws IOException {
        InputStream in = new ByteArrayInputStream(Base64.getDecoder()
                .decode("iVBORw0KGgoAAAANSUhEUn////4AAAABCAMAAABEpIrGAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAA9QTFRFZszM////AAAAM5lmmf/MPkyvFQAAAGFJREFUeNrckzEOwCAMA5OY/7+5NBQJ1DphYaA3sPgkCwtEE0TVAm7BCkfMBaHgp4JvFwjPulSoITAabwHwk1a0PBB6TSBM+bcw5ERIlkQiTEPuqTj2ydWbUWzl8yZcAgwA0mYDNbDXy5oAAAo="));
        ImageInputStream input = ImageIO.createImageInputStream(in);
        ImageReader reader = ImageIO.getImageReadersByFormatName("png").next();
        assert reader.getClass().getName().equals("com.sun.imageio.plugins.png.PNGImageReader");
        ImageReadParam param = reader.getDefaultReadParam();
        reader.setInput(input, true, true);
        reader.read(0, param);
        reader.dispose();

    }
}
