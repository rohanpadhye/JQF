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
package edu.berkeley.cs.jqf.fuzz.ei.state;

import edu.berkeley.cs.jqf.fuzz.ei.ExecutionIndex;
import edu.berkeley.cs.jqf.instrument.tracing.events.AllocEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.BranchEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReadEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReturnEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEventVisitor;

/**
 * ExecutionIndexingState implementation for Janala
 * instrumentation framework.
 *
 * @see ExecutionIndex
 *
 * @author Rohan Padhye
 */
public class JanalaExecutionIndexingState extends AbstractExecutionIndexingState implements TraceEventVisitor {
    public JanalaExecutionIndexingState() {
        super();
    }

    public JanalaExecutionIndexingState(JanalaExecutionIndexingState eis) {
        super(eis);
    }

    @Override
    public void visitCallEvent(CallEvent c) {
        setLastEventIid(c.getIid());
        this.pushCall(c.getIid());
    }

    @Override
    public void visitReturnEvent(ReturnEvent r) {
        setLastEventIid(r.getIid());
        this.popReturn(r.getIid());
    }

    @Override
    public void visitAllocEvent(AllocEvent e) {
        setLastEventIid(e.getIid());
    }

    @Override
    public void visitBranchEvent(BranchEvent e) {
        setLastEventIid(e.getIid());
    }

    @Override
    public void visitReadEvent(ReadEvent e) {
        setLastEventIid(e.getIid());
    }
}
