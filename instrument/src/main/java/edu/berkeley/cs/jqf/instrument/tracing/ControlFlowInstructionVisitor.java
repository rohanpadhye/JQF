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

import janala.logger.inst.*;

/**
 * This is an instruction visitor that has additional convenience methods
 * for visiting control-flow instructions.
 *
 * <p>Additional control-flow groups include conditional jumps,
 * method exits (such as various returns or exceptional exit) and
 * method invocations.</p>
 */
public class ControlFlowInstructionVisitor extends DefaultInstructionVisitor {

    public void visitInvokeInstruction(InvokeInstruction inst) {
        // Nothing by default
    }

    @Override
    public void visitINVOKEINTERFACE(INVOKEINTERFACE inst) {
        visitInvokeInstruction(inst);
        super.visitINVOKEINTERFACE(inst);
    }

    @Override
    public void visitINVOKESPECIAL(INVOKESPECIAL inst) {
        visitInvokeInstruction(inst);
        super.visitINVOKESPECIAL(inst);
    }

    @Override
    public void visitINVOKESTATIC(INVOKESTATIC inst) {
        visitInvokeInstruction(inst);
        super.visitINVOKESTATIC(inst);
    }

    @Override
    public void visitINVOKEVIRTUAL(INVOKEVIRTUAL inst) {
        visitInvokeInstruction(inst);
        super.visitINVOKEVIRTUAL(inst);
    }

    public void visitConditionalBranch(Instruction inst) {
        // Nothing by default
    }

    @Override
    public void visitIFEQ(IFEQ inst) {
        visitConditionalBranch(inst);
        super.visitIFEQ(inst);
    }

    @Override
    public void visitIFGE(IFGE inst) {
        visitConditionalBranch(inst);
        super.visitIFGE(inst);
    }

    @Override
    public void visitIFGT(IFGT inst) {
        visitConditionalBranch(inst);
        super.visitIFGT(inst);
    }

    @Override
    public void visitIFLE(IFLE inst) {
        visitConditionalBranch(inst);
        super.visitIFLE(inst);
    }

    @Override
    public void visitIFLT(IFLT inst) {
        visitConditionalBranch(inst);
        super.visitIFLT(inst);
    }

    @Override
    public void visitIFNE(IFNE inst) {
        visitConditionalBranch(inst);
        super.visitIFNE(inst);
    }

    @Override
    public void visitIFNONNULL(IFNONNULL inst) {
        visitConditionalBranch(inst);
        super.visitIFNONNULL(inst);
    }

    @Override
    public void visitIFNULL(IFNULL inst) {
        visitConditionalBranch(inst);
        super.visitIFNULL(inst);
    }

    @Override
    public void visitIF_ACMPEQ(IF_ACMPEQ inst) {
        visitConditionalBranch(inst);
        super.visitIF_ACMPEQ(inst);
    }

    @Override
    public void visitIF_ACMPNE(IF_ACMPNE inst) {
        visitConditionalBranch(inst);
        super.visitIF_ACMPNE(inst);
    }

    @Override
    public void visitIF_ICMPEQ(IF_ICMPEQ inst) {
        visitConditionalBranch(inst);
        super.visitIF_ICMPEQ(inst);
    }

    @Override
    public void visitIF_ICMPGE(IF_ICMPGE inst) {
        visitConditionalBranch(inst);
        super.visitIF_ICMPGE(inst);
    }

    @Override
    public void visitIF_ICMPGT(IF_ICMPGT inst) {
        visitConditionalBranch(inst);
        super.visitIF_ICMPGT(inst);
    }

    @Override
    public void visitIF_ICMPLE(IF_ICMPLE inst) {
        visitConditionalBranch(inst);
        super.visitIF_ICMPLE(inst);
    }

    @Override
    public void visitIF_ICMPLT(IF_ICMPLT inst) {
        visitConditionalBranch(inst);
        super.visitIF_ICMPLT(inst);
    }

    @Override
    public void visitIF_ICMPNE(IF_ICMPNE inst) {
        visitConditionalBranch(inst);
        super.visitIF_ICMPNE(inst);
    }

    public void visitReturnOrMethodThrow(Instruction inst) {
        // Nothing by default
    }


    @Override
    public void visitARETURN(ARETURN inst) {
        visitReturnOrMethodThrow(inst);
        super.visitARETURN(inst);
    }

    @Override
    public void visitDRETURN(DRETURN inst) {
        visitReturnOrMethodThrow(inst);
        super.visitDRETURN(inst);
    }

    @Override
    public void visitFRETURN(FRETURN inst) {
        visitReturnOrMethodThrow(inst);
        super.visitFRETURN(inst);
    }

    @Override
    public void visitIRETURN(IRETURN inst) {
        visitReturnOrMethodThrow(inst);
        super.visitIRETURN(inst);
    }

    @Override
    public void visitLRETURN(LRETURN inst) {
        visitReturnOrMethodThrow(inst);
        super.visitLRETURN(inst);
    }

    @Override
    public void visitRETURN(RETURN inst) {
        visitReturnOrMethodThrow(inst);
        super.visitRETURN(inst);
    }

    @Override
    public void visitMETHOD_THROW(METHOD_THROW inst) {
        visitReturnOrMethodThrow(inst);
        super.visitMETHOD_THROW(inst);
    }
}
