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
package org.apache.coyote.http11;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.pholser.junit.quickcheck.generator.Size;
import org.apache.coyote.Request;
import org.apache.tomcat.util.net.ApplicationBufferHandler;
import org.apache.tomcat.util.net.SSLSupport;
import org.apache.tomcat.util.net.SendfileDataBase;
import org.apache.tomcat.util.net.SendfileState;
import org.apache.tomcat.util.net.SocketBufferHandler;
import org.apache.tomcat.util.net.SocketWrapperBase;

/**
 * Static class for parsing HTTP headers using Coyote.
 *
 * The sole purpose of this class is to be able to access
 * a package-private method Http11InputBuffer#parseHeaders()
 */
public class HeaderParser {

    private final Http11InputBuffer parser;

    public static final int MAX_HEADER_SIZE = 1024;
    public static final int MAX_BUFFER_SIZE = MAX_HEADER_SIZE + 256;

    public HeaderParser(@Size(max=MAX_HEADER_SIZE) byte[] input) {
        ByteBuffer buf = ByteBuffer.wrap(input);
        Request request = new Request();
        this.parser = new Http11InputBuffer(request, MAX_BUFFER_SIZE,true);
        this.parser.init(new SocketWrapper(buf));
    }

    public void parseHeaders() throws IOException {
        this.parser.parseHeaders();
    }


    private static class SocketWrapper extends SocketWrapperBase<Void> {

        final ByteBuffer in;

        SocketWrapper(ByteBuffer in) {
            super(null, null);
            this.in = in;
            this.socketBufferHandler = new SocketBufferHandler(MAX_BUFFER_SIZE, MAX_BUFFER_SIZE, true);
        }

        @Override
        protected void populateRemoteHost() {

        }

        @Override
        protected void populateRemoteAddr() {

        }

        @Override
        protected void populateRemotePort() {

        }

        @Override
        protected void populateLocalName() {

        }

        @Override
        protected void populateLocalAddr() {

        }

        @Override
        protected void populateLocalPort() {

        }

        @Override
        public int read(boolean block, byte[] b, int off, int len) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read(boolean block, ByteBuffer to) throws IOException {
            int start = in.position();
            to.put(in);
            int end = in.position();
            return end - start;
        }

        @Override
        public boolean isReadyForRead() throws IOException {
            return in.position() < in.limit();
        }

        @Override
        public void setAppReadBufHandler(ApplicationBufferHandler handler) {

        }

        @Override
        public void close() throws IOException {

        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        protected void doWrite(boolean block, ByteBuffer from) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void registerReadInterest() {

        }

        @Override
        public void registerWriteInterest() {

        }

        @Override
        public SendfileDataBase createSendfileData(String filename, long pos, long length) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SendfileState processSendfile(SendfileDataBase sendfileData) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void doClientAuth(SSLSupport sslSupport) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public SSLSupport getSslSupport(String clientCertProvider) {
            throw new UnsupportedOperationException();
        }
    }
}
