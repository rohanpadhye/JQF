package janala.logger.inst;

public interface IVisitor {
  public void visitAALOAD(AALOAD inst);

  public void visitAASTORE(AASTORE inst);

  public void visitACONST_NULL(ACONST_NULL inst);

  public void visitALOAD(ALOAD inst);

  public void visitANEWARRAY(ANEWARRAY inst);

  public void visitARETURN(ARETURN inst);

  public void visitARRAYLENGTH(ARRAYLENGTH inst);

  public void visitASTORE(ASTORE inst);

  public void visitATHROW(ATHROW inst);

  public void visitBALOAD(BALOAD inst);

  public void visitBASTORE(BASTORE inst);

  public void visitBIPUSH(BIPUSH inst);

  public void visitCALOAD(CALOAD inst);

  public void visitCASTORE(CASTORE inst);

  public void visitCHECKCAST(CHECKCAST inst);

  public void visitD2F(D2F inst);

  public void visitD2I(D2I inst);

  public void visitD2L(D2L inst);

  public void visitDADD(DADD inst);

  public void visitDALOAD(DALOAD inst);

  public void visitDASTORE(DASTORE inst);

  public void visitDCMPG(DCMPG inst);

  public void visitDCMPL(DCMPL inst);

  public void visitDCONST_0(DCONST_0 inst);

  public void visitDCONST_1(DCONST_1 inst);

  public void visitDDIV(DDIV inst);

  public void visitDLOAD(DLOAD inst);

  public void visitDMUL(DMUL inst);

  public void visitDNEG(DNEG inst);

  public void visitDREM(DREM inst);

  public void visitDRETURN(DRETURN inst);

  public void visitDSTORE(DSTORE inst);

  public void visitDSUB(DSUB inst);

  public void visitDUP(DUP inst);

  public void visitDUP2(DUP2 inst);

  public void visitDUP2_X1(DUP2_X1 inst);

  public void visitDUP2_X2(DUP2_X2 inst);

  public void visitDUP_X1(DUP_X1 inst);

  public void visitDUP_X2(DUP_X2 inst);

  public void visitF2D(F2D inst);

  public void visitF2I(F2I inst);

  public void visitF2L(F2L inst);

  public void visitFADD(FADD inst);

  public void visitFALOAD(FALOAD inst);

  public void visitFASTORE(FASTORE inst);

  public void visitFCMPG(FCMPG inst);

  public void visitFCMPL(FCMPL inst);

  public void visitFCONST_0(FCONST_0 inst);

  public void visitFCONST_1(FCONST_1 inst);

  public void visitFCONST_2(FCONST_2 inst);

  public void visitFDIV(FDIV inst);

  public void visitFLOAD(FLOAD inst);

  public void visitFMUL(FMUL inst);

  public void visitFNEG(FNEG inst);

  public void visitFREM(FREM inst);

  public void visitFRETURN(FRETURN inst);

  public void visitFSTORE(FSTORE inst);

  public void visitFSUB(FSUB inst);

  public void visitGETFIELD(GETFIELD inst);

  public void visitGETSTATIC(GETSTATIC inst);

  public void visitHEAPLOAD(HEAPLOAD inst);

  public void visitGETVALUE_Object(GETVALUE_Object inst);

  public void visitGETVALUE_boolean(GETVALUE_boolean inst);

  public void visitGETVALUE_byte(GETVALUE_byte inst);

  public void visitGETVALUE_char(GETVALUE_char inst);

  public void visitGETVALUE_double(GETVALUE_double inst);

  public void visitGETVALUE_float(GETVALUE_float inst);

  public void visitGETVALUE_int(GETVALUE_int inst);

  public void visitGETVALUE_long(GETVALUE_long inst);

  public void visitGETVALUE_short(GETVALUE_short inst);

  public void visitGETVALUE_void(GETVALUE_void inst);

  public void visitGOTO(GOTO inst);

  public void visitI2B(I2B inst);

  public void visitI2C(I2C inst);

  public void visitI2D(I2D inst);

  public void visitI2F(I2F inst);

  public void visitI2L(I2L inst);

  public void visitI2S(I2S inst);

  public void visitIADD(IADD inst);

  public void visitIALOAD(IALOAD inst);

  public void visitIAND(IAND inst);

  public void visitIASTORE(IASTORE inst);

  public void visitICONST_0(ICONST_0 inst);

  public void visitICONST_1(ICONST_1 inst);

  public void visitICONST_2(ICONST_2 inst);

  public void visitICONST_3(ICONST_3 inst);

  public void visitICONST_4(ICONST_4 inst);

  public void visitICONST_5(ICONST_5 inst);

  public void visitICONST_M1(ICONST_M1 inst);

  public void visitIDIV(IDIV inst);

  public void visitIFEQ(IFEQ inst);

  public void visitIFGE(IFGE inst);

  public void visitIFGT(IFGT inst);

  public void visitIFLE(IFLE inst);

  public void visitIFLT(IFLT inst);

  public void visitIFNE(IFNE inst);

  public void visitIFNONNULL(IFNONNULL inst);

  public void visitIFNULL(IFNULL inst);

  public void visitIF_ACMPEQ(IF_ACMPEQ inst);

  public void visitIF_ACMPNE(IF_ACMPNE inst);

  public void visitIF_ICMPEQ(IF_ICMPEQ inst);

  public void visitIF_ICMPGE(IF_ICMPGE inst);

  public void visitIF_ICMPGT(IF_ICMPGT inst);

  public void visitIF_ICMPLE(IF_ICMPLE inst);

  public void visitIF_ICMPLT(IF_ICMPLT inst);

  public void visitIF_ICMPNE(IF_ICMPNE inst);

  public void visitIINC(IINC inst);

  public void visitILOAD(ILOAD inst);

  public void visitIMUL(IMUL inst);

  public void visitINEG(INEG inst);

  public void visitINSTANCEOF(INSTANCEOF inst);

  public void visitINVOKEINTERFACE(INVOKEINTERFACE inst);

  public void visitINVOKEMETHOD_EXCEPTION(INVOKEMETHOD_EXCEPTION inst);

  public void visitINVOKESPECIAL(INVOKESPECIAL inst);

  public void visitINVOKESTATIC(INVOKESTATIC inst);

  public void visitINVOKEVIRTUAL(INVOKEVIRTUAL inst);

  public void visitIOR(IOR inst);

  public void visitIREM(IREM inst);

  public void visitIRETURN(IRETURN inst);

  public void visitISHL(ISHL inst);

  public void visitISHR(ISHR inst);

  public void visitISTORE(ISTORE inst);

  public void visitISUB(ISUB inst);

  public void visitIUSHR(IUSHR inst);

  public void visitIXOR(IXOR inst);

  public void visitJSR(JSR inst);

  public void visitL2D(L2D inst);

  public void visitL2F(L2F inst);

  public void visitL2I(L2I inst);

  public void visitLADD(LADD inst);

  public void visitLALOAD(LALOAD inst);

  public void visitLAND(LAND inst);

  public void visitLASTORE(LASTORE inst);

  public void visitLCMP(LCMP inst);

  public void visitLCONST_0(LCONST_0 inst);

  public void visitLCONST_1(LCONST_1 inst);

  public void visitLDC_String(LDC_String inst);

  public void visitLDC_double(LDC_double inst);

  public void visitLDC_float(LDC_float inst);

  public void visitLDC_int(LDC_int inst);

  public void visitLDC_long(LDC_long inst);

  public void visitLDC_Object(LDC_Object inst);

  public void visitLDIV(LDIV inst);

  public void visitLLOAD(LLOAD inst);

  public void visitLMUL(LMUL inst);

  public void visitLNEG(LNEG inst);

  public void visitLOOKUPSWITCH(LOOKUPSWITCH inst);

  public void visitLOR(LOR inst);

  public void visitLREM(LREM inst);

  public void visitLRETURN(LRETURN inst);

  public void visitLSHL(LSHL inst);

  public void visitLSHR(LSHR inst);

  public void visitLSTORE(LSTORE inst);

  public void visitLSUB(LSUB inst);

  public void visitLUSHR(LUSHR inst);

  public void visitLXOR(LXOR inst);

  public void visitMONITORENTER(MONITORENTER inst);

  public void visitMONITOREXIT(MONITOREXIT inst);

  public void visitMULTIANEWARRAY(MULTIANEWARRAY inst);

  public void visitNEW(NEW inst);

  public void visitNEWARRAY(NEWARRAY inst);

  public void visitNOP(NOP inst);

  public void visitPOP(POP inst);

  public void visitPOP2(POP2 inst);

  public void visitPUTFIELD(PUTFIELD inst);

  public void visitPUTSTATIC(PUTSTATIC inst);

  public void visitRET(RET inst);

  public void visitRETURN(RETURN inst);

  public void visitSALOAD(SALOAD inst);

  public void visitSASTORE(SASTORE inst);

  public void visitSIPUSH(SIPUSH inst);

  public void visitSWAP(SWAP inst);

  public void visitTABLESWITCH(TABLESWITCH inst);

  public void visitMETHOD_BEGIN(METHOD_BEGIN inst);

  public void visitMETHOD_THROW(METHOD_THROW inst);

  public void visitINVOKEMETHOD_END(INVOKEMETHOD_END inst);

  public void visitMAKE_SYMBOLIC(MAKE_SYMBOLIC inst);

  public void visitSPECIAL(SPECIAL inst);

  public void setNext(Instruction next);
}
