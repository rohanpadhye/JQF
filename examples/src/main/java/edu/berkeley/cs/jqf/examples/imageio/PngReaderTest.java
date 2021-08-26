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
package edu.berkeley.cs.jqf.examples.imageio;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.generator.Size;
import edu.berkeley.cs.jqf.examples.kaitai.PngKaitaiGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
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
    public void read(ImageInputStream input) throws IOException {
        // Decode image from input stream
        reader.setInput(input);
        // Bound dimensions
        Assume.assumeTrue(reader.getHeight(0) < 1024);
        Assume.assumeTrue(reader.getWidth(0) < 1024);
        // Parse PNG
        reader.read(0);
    }

    @Fuzz
    public void getWidth(ImageInputStream input) throws IOException {
        // Decode image from input stream
        reader.setInput(input);
        int width = reader.getWidth(0);
        System.out.println(width);
    }

    @Fuzz
    public void getHeight(ImageInputStream input) throws IOException {
        // Decode image from input stream
        reader.setInput(input);
        int height = reader.getHeight(0);
        System.out.println(height);
    }

    @Fuzz
    public void debugKaitai(@From(PngKaitaiGenerator.class) @Size(max = 256) InputStream input)  {
        try (FileOutputStream out = new FileOutputStream("kaitai.png")) {
            int val;
            while ((val = input.read()) != -1) {
                out.write(val);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Fuzz
    public void fuzzValidMetadata(@From(PngKaitaiGenerator.class) @Size(max = 256) InputStream input)  {
        // Decode image from input stream
        try {
            reader.setInput(ImageIO.createImageInputStream(input));
            reader.getImageMetadata(0);
        } catch (IOException e) {
            Assume.assumeNoException(e);
        }

    }

    @Fuzz
    public void fuzzValidImage(@From(PngKaitaiGenerator.class) @Size(max = 2048) InputStream input)  {
        // Decode image from input stream
        try {
            reader.setInput(ImageIO.createImageInputStream(input));
            reader.getImageMetadata(0);
            Assume.assumeTrue(reader.getHeight(0) < 1024);
            Assume.assumeTrue(reader.getWidth(0)  < 1024);
        } catch (IOException e) {
            Assume.assumeNoException(e);
        }

    }

    @Fuzz
    public void readUsingKaitai(@From(PngKaitaiGenerator.class) @Size(max = 1024) InputStream input) throws IOException {
        // Decode image from input stream
        reader.setInput(ImageIO.createImageInputStream(input));
        // Bound dimensions
        Assume.assumeTrue(reader.getHeight(0) < 1024);
        Assume.assumeTrue(reader.getWidth(0) < 1024);
        // Parse PNG
        reader.read(0);
    }

}
