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
 * An interface representing by a trace event such as CALL, RETURN or BRANCH.
 *
 *
 * @author Rohan Padhye
 */
public abstract class TraceEvent {

    protected final int iid;
    protected final MemberRef containingMethod;
    protected final int lineNumber;

    public TraceEvent(int iid, MemberRef method, int lineNumber) {
        this.iid = iid;
        this.containingMethod = method;
        this.lineNumber = lineNumber;
    }

    public int getIid() {
        return iid;
    }

    public String getFileName() {
        if (containingMethod == null) {
            return "<unknown>";
        }
        String owner = containingMethod.getOwner();
        int idxOfDollar = owner.indexOf('$');
        if (idxOfDollar >= 0) {
            return owner.substring(0, idxOfDollar) + ".java";
        } else {
            return owner + ".java";
        }
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public String getContainingClass() {
        if (containingMethod == null) {
            return "";
        } else {
            return containingMethod.getOwner();
        }
    }

    public String getContainingMethodName() {
        if (containingMethod == null) {
            return "<unknown>";
        } else {
            return containingMethod.getName();
        }
    }

    public String getContainingMethodDesc() {
        if (containingMethod == null) {
            return "(?)";
        } else {
            return containingMethod.getDesc();
        }
    }

    public abstract void applyVisitor(TraceEventVisitor v);
}
