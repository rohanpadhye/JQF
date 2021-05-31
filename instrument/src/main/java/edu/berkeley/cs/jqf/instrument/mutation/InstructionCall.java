/*
 * Copyright (c) 2021 Isabella Laybourn
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
package edu.berkeley.cs.jqf.instrument.mutation;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Describes an instruction call in bytecode using ASM
 *
 * @author Bella Laybourn
 */
public class InstructionCall implements Opcodes {
    public enum CallType {
        INSN,
        METHOD_INSN,
        LDC_INSN,
        JUMP_INSN
    }

    //for any insn:
    private final CallType type;
    private final int opcode;

    //for methodInsn:
    private String owner;
    private String name;
    private String descriptor;
    private boolean isInterface;

    //for ldcInsn:
    private Object argument;

    /** use for default insn */
    InstructionCall(int op) {
        type = CallType.INSN;
        opcode = op;
    }

    /** use if methodInsn */
    InstructionCall(int op, String ow, String n, String d, boolean i) {
        type = CallType.METHOD_INSN;
        opcode = op;
        owner = ow;
        name = n;
        descriptor = d;
        isInterface = i;
    }

    /** use if ldcInsn */
    InstructionCall(int op, Object arg) {
        if(op == Opcodes.LDC) {
            type = CallType.LDC_INSN;
            opcode = op;
            argument = arg;
        } else {
            type = CallType.JUMP_INSN;
            opcode = op;
        }
    }

    public void call(MethodVisitor mv, Label l) {
        switch(type) {
            case INSN: mv.visitInsn(opcode); break;
            case METHOD_INSN: mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface); break;
            case LDC_INSN: mv.visitLdcInsn(argument); break;
            case JUMP_INSN: mv.visitJumpInsn(opcode, l);
        }
    }

    @Override
    public String toString() {
        return "InstructionCall " + type + " - " + opcode;
    }
}
