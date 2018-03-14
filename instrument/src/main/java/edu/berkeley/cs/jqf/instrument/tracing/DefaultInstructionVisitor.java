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
 * An implementation of the Janala instruction visitor interface
 * that does nothing for each instruction.
 */
public class DefaultInstructionVisitor implements IVisitor {
    @Override
    public void visitAALOAD(AALOAD inst) {

    }

    @Override
    public void visitAASTORE(AASTORE inst) {

    }

    @Override
    public void visitACONST_NULL(ACONST_NULL inst) {

    }

    @Override
    public void visitALOAD(ALOAD inst) {

    }

    @Override
    public void visitANEWARRAY(ANEWARRAY inst) {

    }

    @Override
    public void visitARETURN(ARETURN inst) {

    }

    @Override
    public void visitARRAYLENGTH(ARRAYLENGTH inst) {

    }

    @Override
    public void visitASTORE(ASTORE inst) {

    }

    @Override
    public void visitATHROW(ATHROW inst) {

    }

    @Override
    public void visitBALOAD(BALOAD inst) {

    }

    @Override
    public void visitBASTORE(BASTORE inst) {

    }

    @Override
    public void visitBIPUSH(BIPUSH inst) {

    }

    @Override
    public void visitCALOAD(CALOAD inst) {

    }

    @Override
    public void visitCASTORE(CASTORE inst) {

    }

    @Override
    public void visitCHECKCAST(CHECKCAST inst) {

    }

    @Override
    public void visitD2F(D2F inst) {

    }

    @Override
    public void visitD2I(D2I inst) {

    }

    @Override
    public void visitD2L(D2L inst) {

    }

    @Override
    public void visitDADD(DADD inst) {

    }

    @Override
    public void visitDALOAD(DALOAD inst) {

    }

    @Override
    public void visitDASTORE(DASTORE inst) {

    }

    @Override
    public void visitDCMPG(DCMPG inst) {

    }

    @Override
    public void visitDCMPL(DCMPL inst) {

    }

    @Override
    public void visitDCONST_0(DCONST_0 inst) {

    }

    @Override
    public void visitDCONST_1(DCONST_1 inst) {

    }

    @Override
    public void visitDDIV(DDIV inst) {

    }

    @Override
    public void visitDLOAD(DLOAD inst) {

    }

    @Override
    public void visitDMUL(DMUL inst) {

    }

    @Override
    public void visitDNEG(DNEG inst) {

    }

    @Override
    public void visitDREM(DREM inst) {

    }

    @Override
    public void visitDRETURN(DRETURN inst) {

    }

    @Override
    public void visitDSTORE(DSTORE inst) {

    }

    @Override
    public void visitDSUB(DSUB inst) {

    }

    @Override
    public void visitDUP(DUP inst) {

    }

    @Override
    public void visitDUP2(DUP2 inst) {

    }

    @Override
    public void visitDUP2_X1(DUP2_X1 inst) {

    }

    @Override
    public void visitDUP2_X2(DUP2_X2 inst) {

    }

    @Override
    public void visitDUP_X1(DUP_X1 inst) {

    }

    @Override
    public void visitDUP_X2(DUP_X2 inst) {

    }

    @Override
    public void visitF2D(F2D inst) {

    }

    @Override
    public void visitF2I(F2I inst) {

    }

    @Override
    public void visitF2L(F2L inst) {

    }

    @Override
    public void visitFADD(FADD inst) {

    }

    @Override
    public void visitFALOAD(FALOAD inst) {

    }

    @Override
    public void visitFASTORE(FASTORE inst) {

    }

    @Override
    public void visitFCMPG(FCMPG inst) {

    }

    @Override
    public void visitFCMPL(FCMPL inst) {

    }

    @Override
    public void visitFCONST_0(FCONST_0 inst) {

    }

    @Override
    public void visitFCONST_1(FCONST_1 inst) {

    }

    @Override
    public void visitFCONST_2(FCONST_2 inst) {

    }

    @Override
    public void visitFDIV(FDIV inst) {

    }

    @Override
    public void visitFLOAD(FLOAD inst) {

    }

    @Override
    public void visitFMUL(FMUL inst) {

    }

    @Override
    public void visitFNEG(FNEG inst) {

    }

    @Override
    public void visitFREM(FREM inst) {

    }

    @Override
    public void visitFRETURN(FRETURN inst) {

    }

    @Override
    public void visitFSTORE(FSTORE inst) {

    }

    @Override
    public void visitFSUB(FSUB inst) {

    }

    @Override
    public void visitGETFIELD(GETFIELD inst) {

    }

    @Override
    public void visitGETSTATIC(GETSTATIC inst) {

    }

    @Override
    public void visitHEAPLOAD(HEAPLOAD inst) {

    }

    @Override
    public void visitGETVALUE_Object(GETVALUE_Object inst) {

    }

    @Override
    public void visitGETVALUE_boolean(GETVALUE_boolean inst) {

    }

    @Override
    public void visitGETVALUE_byte(GETVALUE_byte inst) {

    }

    @Override
    public void visitGETVALUE_char(GETVALUE_char inst) {

    }

    @Override
    public void visitGETVALUE_double(GETVALUE_double inst) {

    }

    @Override
    public void visitGETVALUE_float(GETVALUE_float inst) {

    }

    @Override
    public void visitGETVALUE_int(GETVALUE_int inst) {

    }

    @Override
    public void visitGETVALUE_long(GETVALUE_long inst) {

    }

    @Override
    public void visitGETVALUE_short(GETVALUE_short inst) {

    }

    @Override
    public void visitGETVALUE_void(GETVALUE_void inst) {

    }

    @Override
    public void visitGOTO(GOTO inst) {

    }

    @Override
    public void visitI2B(I2B inst) {

    }

    @Override
    public void visitI2C(I2C inst) {

    }

    @Override
    public void visitI2D(I2D inst) {

    }

    @Override
    public void visitI2F(I2F inst) {

    }

    @Override
    public void visitI2L(I2L inst) {

    }

    @Override
    public void visitI2S(I2S inst) {

    }

    @Override
    public void visitIADD(IADD inst) {

    }

    @Override
    public void visitIALOAD(IALOAD inst) {

    }

    @Override
    public void visitIAND(IAND inst) {

    }

    @Override
    public void visitIASTORE(IASTORE inst) {

    }

    @Override
    public void visitICONST_0(ICONST_0 inst) {

    }

    @Override
    public void visitICONST_1(ICONST_1 inst) {

    }

    @Override
    public void visitICONST_2(ICONST_2 inst) {

    }

    @Override
    public void visitICONST_3(ICONST_3 inst) {

    }

    @Override
    public void visitICONST_4(ICONST_4 inst) {

    }

    @Override
    public void visitICONST_5(ICONST_5 inst) {

    }

    @Override
    public void visitICONST_M1(ICONST_M1 inst) {

    }

    @Override
    public void visitIDIV(IDIV inst) {

    }

    @Override
    public void visitIFEQ(IFEQ inst) {

    }

    @Override
    public void visitIFGE(IFGE inst) {

    }

    @Override
    public void visitIFGT(IFGT inst) {

    }

    @Override
    public void visitIFLE(IFLE inst) {

    }

    @Override
    public void visitIFLT(IFLT inst) {

    }

    @Override
    public void visitIFNE(IFNE inst) {

    }

    @Override
    public void visitIFNONNULL(IFNONNULL inst) {

    }

    @Override
    public void visitIFNULL(IFNULL inst) {

    }

    @Override
    public void visitIF_ACMPEQ(IF_ACMPEQ inst) {

    }

    @Override
    public void visitIF_ACMPNE(IF_ACMPNE inst) {

    }

    @Override
    public void visitIF_ICMPEQ(IF_ICMPEQ inst) {

    }

    @Override
    public void visitIF_ICMPGE(IF_ICMPGE inst) {

    }

    @Override
    public void visitIF_ICMPGT(IF_ICMPGT inst) {

    }

    @Override
    public void visitIF_ICMPLE(IF_ICMPLE inst) {

    }

    @Override
    public void visitIF_ICMPLT(IF_ICMPLT inst) {

    }

    @Override
    public void visitIF_ICMPNE(IF_ICMPNE inst) {

    }

    @Override
    public void visitIINC(IINC inst) {

    }

    @Override
    public void visitILOAD(ILOAD inst) {

    }

    @Override
    public void visitIMUL(IMUL inst) {

    }

    @Override
    public void visitINEG(INEG inst) {

    }

    @Override
    public void visitINSTANCEOF(INSTANCEOF inst) {

    }

    @Override
    public void visitINVOKEINTERFACE(INVOKEINTERFACE inst) {

    }

    @Override
    public void visitINVOKEMETHOD_EXCEPTION(INVOKEMETHOD_EXCEPTION inst) {

    }

    @Override
    public void visitINVOKESPECIAL(INVOKESPECIAL inst) {

    }

    @Override
    public void visitINVOKESTATIC(INVOKESTATIC inst) {

    }

    @Override
    public void visitINVOKEVIRTUAL(INVOKEVIRTUAL inst) {

    }

    @Override
    public void visitIOR(IOR inst) {

    }

    @Override
    public void visitIREM(IREM inst) {

    }

    @Override
    public void visitIRETURN(IRETURN inst) {

    }

    @Override
    public void visitISHL(ISHL inst) {

    }

    @Override
    public void visitISHR(ISHR inst) {

    }

    @Override
    public void visitISTORE(ISTORE inst) {

    }

    @Override
    public void visitISUB(ISUB inst) {

    }

    @Override
    public void visitIUSHR(IUSHR inst) {

    }

    @Override
    public void visitIXOR(IXOR inst) {

    }

    @Override
    public void visitJSR(JSR inst) {

    }

    @Override
    public void visitL2D(L2D inst) {

    }

    @Override
    public void visitL2F(L2F inst) {

    }

    @Override
    public void visitL2I(L2I inst) {

    }

    @Override
    public void visitLADD(LADD inst) {

    }

    @Override
    public void visitLALOAD(LALOAD inst) {

    }

    @Override
    public void visitLAND(LAND inst) {

    }

    @Override
    public void visitLASTORE(LASTORE inst) {

    }

    @Override
    public void visitLCMP(LCMP inst) {

    }

    @Override
    public void visitLCONST_0(LCONST_0 inst) {

    }

    @Override
    public void visitLCONST_1(LCONST_1 inst) {

    }

    @Override
    public void visitLDC_String(LDC_String inst) {

    }

    @Override
    public void visitLDC_double(LDC_double inst) {

    }

    @Override
    public void visitLDC_float(LDC_float inst) {

    }

    @Override
    public void visitLDC_int(LDC_int inst) {

    }

    @Override
    public void visitLDC_long(LDC_long inst) {

    }

    @Override
    public void visitLDC_Object(LDC_Object inst) {

    }

    @Override
    public void visitLDIV(LDIV inst) {

    }

    @Override
    public void visitLLOAD(LLOAD inst) {

    }

    @Override
    public void visitLMUL(LMUL inst) {

    }

    @Override
    public void visitLNEG(LNEG inst) {

    }

    @Override
    public void visitLOOKUPSWITCH(LOOKUPSWITCH inst) {

    }

    @Override
    public void visitLOR(LOR inst) {

    }

    @Override
    public void visitLREM(LREM inst) {

    }

    @Override
    public void visitLRETURN(LRETURN inst) {

    }

    @Override
    public void visitLSHL(LSHL inst) {

    }

    @Override
    public void visitLSHR(LSHR inst) {

    }

    @Override
    public void visitLSTORE(LSTORE inst) {

    }

    @Override
    public void visitLSUB(LSUB inst) {

    }

    @Override
    public void visitLUSHR(LUSHR inst) {

    }

    @Override
    public void visitLXOR(LXOR inst) {

    }

    @Override
    public void visitMONITORENTER(MONITORENTER inst) {

    }

    @Override
    public void visitMONITOREXIT(MONITOREXIT inst) {

    }

    @Override
    public void visitMULTIANEWARRAY(MULTIANEWARRAY inst) {

    }

    @Override
    public void visitNEW(NEW inst) {

    }

    @Override
    public void visitNEWARRAY(NEWARRAY inst) {

    }

    @Override
    public void visitNOP(NOP inst) {

    }

    @Override
    public void visitPOP(POP inst) {

    }

    @Override
    public void visitPOP2(POP2 inst) {

    }

    @Override
    public void visitPUTFIELD(PUTFIELD inst) {

    }

    @Override
    public void visitPUTSTATIC(PUTSTATIC inst) {

    }

    @Override
    public void visitRET(RET inst) {

    }

    @Override
    public void visitRETURN(RETURN inst) {

    }

    @Override
    public void visitSALOAD(SALOAD inst) {

    }

    @Override
    public void visitSASTORE(SASTORE inst) {

    }

    @Override
    public void visitSIPUSH(SIPUSH inst) {

    }

    @Override
    public void visitSWAP(SWAP inst) {

    }

    @Override
    public void visitTABLESWITCH(TABLESWITCH inst) {

    }

    @Override
    public void visitMETHOD_BEGIN(METHOD_BEGIN inst) {

    }

    @Override
    public void visitMETHOD_THROW(METHOD_THROW inst) {

    }

    @Override
    public void visitINVOKEMETHOD_END(INVOKEMETHOD_END inst) {

    }

    @Override
    public void visitMAKE_SYMBOLIC(MAKE_SYMBOLIC inst) {

    }

    @Override
    public void visitSPECIAL(SPECIAL inst) {

    }

    @Override
    public void setNext(Instruction next) {

    }
}
