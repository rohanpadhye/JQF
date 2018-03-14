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

package edu.berkeley.cs.jqf.instrument.tracing;

import java.io.*;

/** @author Rohan Padhye */
public class PrintLogger {
    private final PrintWriter writer;

    private PrintLogger(String name, OutputStream out) {
        this.writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(out)));
    }

    public PrintLogger(String name) {
        this(name, createOutputStream(name));
    }

    private static OutputStream createOutputStream(String name) {
        try {
            return new FileOutputStream(name + ".log");
        } catch (IOException e) {
            e.printStackTrace();
            return new OutputStream() {
                @Override
                public void write(int b) throws IOException {
                     // Do nothing
                }
            };
        }
    }

    public void log(String info) {
        writer.println(info);
        writer.flush();
    }

    public PrintWriter getWriter() {
        return this.writer;
    }

    public void close() {
        writer.flush();
        writer.close();
    };
}
