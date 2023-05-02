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
package edu.berkeley.cs.jqf.instrument.tracing.events;

import janala.logger.inst.MemberRef;

/**
 * @author Rohan Padhye
 */
public class CallEvent extends TraceEvent {
    protected final MemberRef invokedMethod;
    private String str;
    private final Object obj;

    public CallEvent(int iid, MemberRef containingMethod, int lineNumber, MemberRef invokedMethod) {
        super(iid, containingMethod, lineNumber);
        this.invokedMethod = invokedMethod;
        this.obj = null;
    }

    public CallEvent(int iid, MemberRef containingMethod, int lineNumber, MemberRef invokedMethod, Object callingObject) {
        super(iid, containingMethod, lineNumber);
        this.invokedMethod = invokedMethod;
        this.obj = callingObject;
    }

    public String getInvokedMethodName() {
        if (str == null) {
            this.str = invokedMethod.getOwner() + "#" + invokedMethod.getName() + invokedMethod.getDesc();
        }
        return str;
    }

    public Object getCallingObject() {
        return obj;
    }

    @Override
    public String toString() {
        return String.format("CALL(%d,%d,%s)", iid, lineNumber, getInvokedMethodName());
    }

    @Override
    public void applyVisitor(TraceEventVisitor v) {
        v.visitCallEvent(this);
    }
}
