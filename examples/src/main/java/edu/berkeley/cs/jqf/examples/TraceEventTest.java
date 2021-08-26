/*
 * Copyright (c) 2019, The Regents of the University of California
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
package edu.berkeley.cs.jqf.examples;

import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.berkeley.cs.jqf.instrument.tracing.TraceLogger;
import edu.berkeley.cs.jqf.instrument.tracing.events.BranchEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEventVisitor;
import janala.logger.inst.METHOD_BEGIN;
import janala.logger.inst.MemberRef;
import org.junit.runner.RunWith;

/**
 * @author Rohan Padhye
 */
@RunWith(JQF.class)
public class TraceEventTest {

    @Fuzz
    public void fakeBranchEvent(int iid, int arm) {
        TraceEvent e = new BranchEvent(iid,
                new METHOD_BEGIN("examples.A", "foo", "()V"), 0, arm);
        TraceLogger.get().emit(e);
    }

    @Fuzz
    public void customEvent(String data) {
        int iid = TraceEventTest.class.hashCode(); // this should be a random value associated with a program location
        MemberRef method = new METHOD_BEGIN("examples.A", "foo", "()V"); // containing method
        int lineNumber = 0; // line number if it exists

        // Generate a custom event!
        TraceLogger.get().emit(new CustomEvent(iid, method, lineNumber, data));

    }

    private static class CustomEvent extends TraceEvent {

        private String data;

        public CustomEvent(int iid, MemberRef method, int lineNumber, String data) {
            super(iid, method, lineNumber);
            this.data = data;
        }

        @Override
        public void applyVisitor(TraceEventVisitor v) {
            throw new UnsupportedOperationException("No visitor method for custom event");
        }
    }
}
