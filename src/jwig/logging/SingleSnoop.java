package jwig.logging;

import janala.config.Config;
import janala.logger.Logger;

@SuppressWarnings("unused") // Dynamically loaded
public final class SingleSnoop {
  private static Logger intp = Config.instance.getLogger();
  private static boolean block = true;

  private SingleSnoop() {} 

  // For testing purposes
  public static void setInterpreter(Logger logger) {
    intp = logger;
  }

  public static void LDC(int iid, int mid, int c) {
    if (block) return; else block = true;
    intp.LDC(iid, mid, c); block = false;
  }

  public static void LDC(int iid, int mid, long c) {
    if (block) return; else block = true;
    intp.LDC(iid, mid, c); block = false;
  }

  public static void LDC(int iid, int mid, float c) {
    if (block) return; else block = true;
    intp.LDC(iid, mid, c); block = false;
  }
  
  public static void LDC(int iid, int mid, double c) {
    if (block) return; else block = true;
    intp.LDC(iid, mid, c); block = false;
  }
  
  public static void LDC(int iid, int mid, String c) {
    if (block) return; else block = true;
    intp.LDC(iid, mid, c); block = false;
  }

  public static void LDC(int iid, int mid, Object c) {
    if (block) return; else block = true;
    intp.LDC(iid, mid, c); block = false;
  }

  public static void IINC(int iid, int mid, int var, int increment) {
    if (block) return; else block = true;
    intp.IINC(iid, mid, var, increment); block = false;
  }

  public static void MULTIANEWARRAY(int iid, int mid, String desc, int dims) {
    if (block) return; else block = true;
    intp.MULTIANEWARRAY(iid, mid, desc, dims); block = false;
  }

  public static void LOOKUPSWITCH(int iid, int mid, int dflt, int[] keys, int[] labels) {
    if (block) return; else block = true;
    intp.LOOKUPSWITCH(iid, mid, dflt, keys, labels); block = false;
  }

  public static void TABLESWITCH(int iid, int mid, int min, int max, int dflt, int[] labels) {
    if (block) return; else block = true;
    intp.TABLESWITCH(iid, mid, min, max, dflt, labels); block = false;
  }

  public static void IFEQ(int iid, int mid, int label) {
    if (block) return; else block = true;
    intp.IFEQ(iid, mid, label); block = false;
  }

  public static void IFNE(int iid, int mid, int label) {
    if (block) return; else block = true;
    intp.IFNE(iid, mid, label); block = false;
  }

  public static void IFLT(int iid, int mid, int label) {
    if (block) return; else block = true;
    intp.IFLT(iid, mid, label); block = false;
  }

  public static void IFGE(int iid, int mid, int label) {
    if (block) return; else block = true;
    intp.IFGE(iid, mid, label); block = false;
  }

  public static void IFGT(int iid, int mid, int label) {
    if (block) return; else block = true;
    intp.IFGT(iid, mid, label); block = false;
  }

  public static void IFLE(int iid, int mid, int label) {
    if (block) return; else block = true;
    intp.IFLE(iid, mid, label); block = false;
  }

  public static void IF_ICMPEQ(int iid, int mid, int label) {
    if (block) return; else block = true;
    intp.IF_ICMPEQ(iid, mid, label); block = false;
  }

  public static void IF_ICMPNE(int iid, int mid, int label) {
    if (block) return; else block = true;
    intp.IF_ICMPNE(iid, mid, label); block = false;
  }

  public static void IF_ICMPLT(int iid, int mid, int label) {
    if (block) return; else block = true;
    intp.IF_ICMPLT(iid, mid, label); block = false;
  }

  public static void IF_ICMPGE(int iid, int mid, int label) {
    if (block) return; else block = true;
    intp.IF_ICMPGE(iid, mid, label); block = false;
  }

  public static void IF_ICMPGT(int iid, int mid, int label) {
    if (block) return; else block = true;
    intp.IF_ICMPGT(iid, mid, label); block = false;
  }

  public static void IF_ICMPLE(int iid, int mid, int label) {
    if (block) return; else block = true;
    intp.IF_ICMPLE(iid, mid, label); block = false;
  }

  public static void IF_ACMPEQ(int iid, int mid, int label) {
    if (block) return; else block = true;
    intp.IF_ACMPEQ(iid, mid, label); block = false;
  }

  public static void IF_ACMPNE(int iid, int mid, int label) {
    if (block) return; else block = true;
    intp.IF_ACMPNE(iid, mid, label); block = false;
  }

  public static void GOTO(int iid, int mid, int label) {
    if (block) return; else block = true;
    intp.GOTO(iid, mid, label); block = false;
  }

  public static void JSR(int iid, int mid, int label) {
    if (block) return; else block = true;
    intp.JSR(iid, mid, label); block = false;
  }

  public static void IFNULL(int iid, int mid, int label) {
    if (block) return; else block = true;
    intp.IFNULL(iid, mid, label); block = false;
  }

  public static void IFNONNULL(int iid, int mid, int label) {
    if (block) return; else block = true;
    intp.IFNONNULL(iid, mid, label); block = false;
  }

  public static void INVOKEVIRTUAL(int iid, int mid, String owner, String name, String desc) {
    if (block) return; else block = true;
    intp.INVOKEVIRTUAL(iid, mid, owner, name, desc); block = false;
  }

  public static void INVOKESPECIAL(int iid, int mid, String owner, String name, String desc) {
    if (block) return; else block = true;
    intp.INVOKESPECIAL(iid, mid, owner, name, desc); block = false;
  }

  public static void INVOKESTATIC(int iid, int mid, String owner, String name, String desc) {
    if (block) return; else block = true;
    intp.INVOKESTATIC(iid, mid, owner, name, desc); block = false;
  }

  public static void INVOKEINTERFACE(int iid, int mid, String owner, String name, String desc) {
    if (block) return; else block = true;
    intp.INVOKEINTERFACE(iid, mid, owner, name, desc); block = false;
  }

  public static void GETSTATIC(int iid, int mid, int cIdx, int fIdx, String desc) {
    if (block) return; else block = true;
    intp.GETSTATIC(iid, mid, cIdx, fIdx, desc); block = false;
  }

  public static void PUTSTATIC(int iid, int mid, int cIdx, int fIdx, String desc) {
    if (block) return; else block = true;
    intp.PUTSTATIC(iid, mid, cIdx, fIdx, desc); block = false;
  }

  public static void GETFIELD(int iid, int mid, int cIdx, int fIdx, String desc) {
    if (block) return; else block = true;
    intp.GETFIELD(iid, mid, cIdx, fIdx, desc); block = false;
  }

  public static void PUTFIELD(int iid, int mid, int cIdx, int fIdx, String desc) {
    if (block) return; else block = true;
    intp.PUTFIELD(iid, mid, cIdx, fIdx, desc); block = false;
  }

  public static void NEW(int iid, int mid, String type, int cIdx) {
    if (block) return; else block = true;
    intp.NEW(iid, mid, type, cIdx); block = false;
  }

  public static void ANEWARRAY(int iid, int mid, String type) {
    if (block) return; else block = true;
    intp.ANEWARRAY(iid, mid, type); block = false;
  }

  public static void CHECKCAST(int iid, int mid, String type) {
    if (block) return; else block = true;
    intp.CHECKCAST(iid, mid, type); block = false;
  }

  public static void INSTANCEOF(int iid, int mid, String type) {
    if (block) return; else block = true;
    intp.INSTANCEOF(iid, mid, type); block = false;
  }

  public static void BIPUSH(int iid, int mid, int value) {
    if (block) return; else block = true;
    intp.BIPUSH(iid, mid, value); block = false;
  }

  public static void SIPUSH(int iid, int mid, int value) {
    if (block) return; else block = true;
    intp.SIPUSH(iid, mid, value); block = false;
  }

  public static void NEWARRAY(int iid, int mid) {
    if (block) return; else block = true;
    intp.NEWARRAY(iid, mid); block = false;
  }

  public static void ILOAD(int iid, int mid, int var) {
    if (block) return; else block = true;
    intp.ILOAD(iid, mid, var); block = false;
  }

  public static void LLOAD(int iid, int mid, int var) {
    if (block) return; else block = true;
    intp.LLOAD(iid, mid, var); block = false;
  }

  public static void FLOAD(int iid, int mid, int var) {
    if (block) return; else block = true;
    intp.FLOAD(iid, mid, var); block = false;
  }

  public static void DLOAD(int iid, int mid, int var) {
    if (block) return; else block = true;
    intp.DLOAD(iid, mid, var); block = false;
  }

  public static void ALOAD(int iid, int mid, int var) {
    if (block) return; else block = true;
    intp.ALOAD(iid, mid, var); block = false;
  }

  public static void ISTORE(int iid, int mid, int var) {
    if (block) return; else block = true;
    intp.ISTORE(iid, mid, var); block = false;
  }

  public static void LSTORE(int iid, int mid, int var) {
    if (block) return; else block = true;
    intp.LSTORE(iid, mid, var); block = false;
  }

  public static void FSTORE(int iid, int mid, int var) {
    if (block) return; else block = true;
    intp.FSTORE(iid, mid, var); block = false;
  }

  public static void DSTORE(int iid, int mid, int var) {
    if (block) return; else block = true;
    intp.DSTORE(iid, mid, var); block = false;
  }

  public static void ASTORE(int iid, int mid, int var) {
    if (block) return; else block = true;
    intp.ASTORE(iid, mid, var); block = false;
  }

  public static void RET(int iid, int mid, int var) {
    if (block) return; else block = true;
    intp.RET(iid, mid, var); block = false;
  }

  public static void NOP(int iid, int mid) {
    if (block) return; else block = true;
    intp.NOP(iid, mid); block = false;
  }

  public static void ACONST_NULL(int iid, int mid) {
    if (block) return; else block = true;
    intp.ACONST_NULL(iid, mid); block = false;
  }

  public static void ICONST_M1(int iid, int mid) {
    if (block) return; else block = true;
    intp.ICONST_M1(iid, mid); block = false;
  }

  public static void ICONST_0(int iid, int mid) {
    if (block) return; else block = true;
    intp.ICONST_0(iid, mid); block = false;
  }

  public static void ICONST_1(int iid, int mid) {
    if (block) return; else block = true;
    intp.ICONST_1(iid, mid); block = false;
  }

  public static void ICONST_2(int iid, int mid) {
    if (block) return; else block = true;
    intp.ICONST_2(iid, mid); block = false;
  }

  public static void ICONST_3(int iid, int mid) {
    if (block) return; else block = true;
    intp.ICONST_3(iid, mid); block = false;
  }

  public static void ICONST_4(int iid, int mid) {
    if (block) return; else block = true;
    intp.ICONST_4(iid, mid); block = false;
  }

  public static void ICONST_5(int iid, int mid) {
    if (block) return; else block = true;
    intp.ICONST_5(iid, mid); block = false;
  }

  public static void LCONST_0(int iid, int mid) {
    if (block) return; else block = true;
    intp.LCONST_0(iid, mid); block = false;
  }

  public static void LCONST_1(int iid, int mid) {
    if (block) return; else block = true;
    intp.LCONST_1(iid, mid); block = false;
  }

  public static void FCONST_0(int iid, int mid) {
    if (block) return; else block = true;
    intp.FCONST_0(iid, mid); block = false;
  }

  public static void FCONST_1(int iid, int mid) {
    if (block) return; else block = true;
    intp.FCONST_1(iid, mid); block = false;
  }

  public static void FCONST_2(int iid, int mid) {
    if (block) return; else block = true;
    intp.FCONST_2(iid, mid); block = false;
  }

  public static void DCONST_0(int iid, int mid) {
    if (block) return; else block = true;
    intp.DCONST_0(iid, mid); block = false;
  }

  public static void DCONST_1(int iid, int mid) {
    if (block) return; else block = true;
    intp.DCONST_1(iid, mid); block = false;
  }

  public static void IALOAD(int iid, int mid) {
    if (block) return; else block = true;
    intp.IALOAD(iid, mid); block = false;
  }

  public static void LALOAD(int iid, int mid) {
    if (block) return; else block = true;
    intp.LALOAD(iid, mid); block = false;
  }

  public static void FALOAD(int iid, int mid) {
    if (block) return; else block = true;
    intp.FALOAD(iid, mid); block = false;
  }

  public static void DALOAD(int iid, int mid) {
    if (block) return; else block = true;
    intp.DALOAD(iid, mid); block = false;
  }

  public static void AALOAD(int iid, int mid) {
    if (block) return; else block = true;
    intp.AALOAD(iid, mid); block = false;
  }

  public static void BALOAD(int iid, int mid) {
    if (block) return; else block = true;
    intp.BALOAD(iid, mid); block = false;
  }

  public static void CALOAD(int iid, int mid) {
    if (block) return; else block = true;
    intp.CALOAD(iid, mid); block = false;
  }

  public static void SALOAD(int iid, int mid) {
    if (block) return; else block = true;
    intp.SALOAD(iid, mid); block = false;
  }

  public static void IASTORE(int iid, int mid) {
    if (block) return; else block = true;
    intp.IASTORE(iid, mid); block = false;
  }

  public static void LASTORE(int iid, int mid) {
    if (block) return; else block = true;
    intp.LASTORE(iid, mid); block = false;
  }

  public static void FASTORE(int iid, int mid) {
    if (block) return; else block = true;
    intp.FASTORE(iid, mid); block = false;
  }

  public static void DASTORE(int iid, int mid) {
    if (block) return; else block = true;
    intp.DASTORE(iid, mid); block = false;
  }

  public static void AASTORE(int iid, int mid) {
    if (block) return; else block = true;
    intp.AASTORE(iid, mid); block = false;
  }

  public static void BASTORE(int iid, int mid) {
    if (block) return; else block = true;
    intp.BASTORE(iid, mid); block = false;
  }

  public static void CASTORE(int iid, int mid) {
    if (block) return; else block = true;
    intp.CASTORE(iid, mid); block = false;
  }

  public static void SASTORE(int iid, int mid) {
    if (block) return; else block = true;
    intp.SASTORE(iid, mid); block = false;
  }

  public static void POP(int iid, int mid) {
    if (block) return; else block = true;
    intp.POP(iid, mid); block = false;
  }

  public static void POP2(int iid, int mid) {
    if (block) return; else block = true;
    intp.POP2(iid, mid); block = false;
  }

  public static void DUP(int iid, int mid) {
    if (block) return; else block = true;
    intp.DUP(iid, mid); block = false;
  }

  public static void DUP_X1(int iid, int mid) {
    if (block) return; else block = true;
    intp.DUP_X1(iid, mid); block = false;
  }

  public static void DUP_X2(int iid, int mid) {
    if (block) return; else block = true;
    intp.DUP_X2(iid, mid); block = false;
  }

  public static void DUP2(int iid, int mid) {
    if (block) return; else block = true;
    intp.DUP2(iid, mid); block = false;
  }

  public static void DUP2_X1(int iid, int mid) {
    if (block) return; else block = true;
    intp.DUP2_X1(iid, mid); block = false;
  }

  public static void DUP2_X2(int iid, int mid) {
    if (block) return; else block = true;
    intp.DUP2_X2(iid, mid); block = false;
  }

  public static void SWAP(int iid, int mid) {
    if (block) return; else block = true;
    intp.SWAP(iid, mid); block = false;
  }

  public static void IADD(int iid, int mid) {
    if (block) return; else block = true;
    intp.IADD(iid, mid); block = false;
  }

  public static void LADD(int iid, int mid) {
    if (block) return; else block = true;
    intp.LADD(iid, mid); block = false;
  }

  public static void FADD(int iid, int mid) {
    if (block) return; else block = true;
    intp.FADD(iid, mid); block = false;
  }

  public static void DADD(int iid, int mid) {
    if (block) return; else block = true;
    intp.DADD(iid, mid); block = false;
  }

  public static void ISUB(int iid, int mid) {
    if (block) return; else block = true;
    intp.ISUB(iid, mid); block = false;
  }

  public static void LSUB(int iid, int mid) {
    if (block) return; else block = true;
    intp.LSUB(iid, mid); block = false;
  }

  public static void FSUB(int iid, int mid) {
    if (block) return; else block = true;
    intp.FSUB(iid, mid); block = false;
  }

  public static void DSUB(int iid, int mid) {
    if (block) return; else block = true;
    intp.DSUB(iid, mid); block = false;
  }

  public static void IMUL(int iid, int mid) {
    if (block) return; else block = true;
    intp.IMUL(iid, mid); block = false;
  }

  public static void LMUL(int iid, int mid) {
    if (block) return; else block = true;
    intp.LMUL(iid, mid); block = false;
  }

  public static void FMUL(int iid, int mid) {
    if (block) return; else block = true;
    intp.FMUL(iid, mid); block = false;
  }

  public static void DMUL(int iid, int mid) {
    if (block) return; else block = true;
    intp.DMUL(iid, mid); block = false;
  }

  public static void IDIV(int iid, int mid) {
    if (block) return; else block = true;
    intp.IDIV(iid, mid); block = false;
  }

  public static void LDIV(int iid, int mid) {
    if (block) return; else block = true;
    intp.LDIV(iid, mid); block = false;
  }

  public static void FDIV(int iid, int mid) {
    if (block) return; else block = true;
    intp.FDIV(iid, mid); block = false;
  }

  public static void DDIV(int iid, int mid) {
    if (block) return; else block = true;
    intp.DDIV(iid, mid); block = false;
  }

  public static void IREM(int iid, int mid) {
    if (block) return; else block = true;
    intp.IREM(iid, mid); block = false;
  }

  public static void LREM(int iid, int mid) {
    if (block) return; else block = true;
    intp.LREM(iid, mid); block = false;
  }

  public static void FREM(int iid, int mid) {
    if (block) return; else block = true;
    intp.FREM(iid, mid); block = false;
  }

  public static void DREM(int iid, int mid) {
    if (block) return; else block = true;
    intp.DREM(iid, mid); block = false;
  }

  public static void INEG(int iid, int mid) {
    if (block) return; else block = true;
    intp.INEG(iid, mid); block = false;
  }

  public static void LNEG(int iid, int mid) {
    if (block) return; else block = true;
    intp.LNEG(iid, mid); block = false;
  }

  public static void FNEG(int iid, int mid) {
    if (block) return; else block = true;
    intp.FNEG(iid, mid); block = false;
  }

  public static void DNEG(int iid, int mid) {
    if (block) return; else block = true;
    intp.DNEG(iid, mid); block = false;
  }

  public static void ISHL(int iid, int mid) {
    if (block) return; else block = true;
    intp.ISHL(iid, mid); block = false;
  }

  public static void LSHL(int iid, int mid) {
    if (block) return; else block = true;
    intp.LSHL(iid, mid); block = false;
  }

  public static void ISHR(int iid, int mid) {
    if (block) return; else block = true;
    intp.ISHR(iid, mid); block = false;
  }

  public static void LSHR(int iid, int mid) {
    if (block) return; else block = true;
    intp.LSHR(iid, mid); block = false;
  }

  public static void IUSHR(int iid, int mid) {
    if (block) return; else block = true;
    intp.IUSHR(iid, mid); block = false;
  }

  public static void LUSHR(int iid, int mid) {
    if (block) return; else block = true;
    intp.LUSHR(iid, mid); block = false;
  }

  public static void IAND(int iid, int mid) {
    if (block) return; else block = true;
    intp.IAND(iid, mid); block = false;
  }

  public static void LAND(int iid, int mid) {
    if (block) return; else block = true;
    intp.LAND(iid, mid); block = false;
  }

  public static void IOR(int iid, int mid) {
    if (block) return; else block = true;
    intp.IOR(iid, mid); block = false;
  }

  public static void LOR(int iid, int mid) {
    if (block) return; else block = true;
    intp.LOR(iid, mid); block = false;
  }

  public static void IXOR(int iid, int mid) {
    if (block) return; else block = true;
    intp.IXOR(iid, mid); block = false;
  }

  public static void LXOR(int iid, int mid) {
    if (block) return; else block = true;
    intp.LXOR(iid, mid); block = false;
  }

  public static void I2L(int iid, int mid) {
    if (block) return; else block = true;
    intp.I2L(iid, mid); block = false;
  }

  public static void I2F(int iid, int mid) {
    if (block) return; else block = true;
    intp.I2F(iid, mid); block = false;
  }

  public static void I2D(int iid, int mid) {
    if (block) return; else block = true;
    intp.I2D(iid, mid); block = false;
  }

  public static void L2I(int iid, int mid) {
    if (block) return; else block = true;
    intp.L2I(iid, mid); block = false;
  }

  public static void L2F(int iid, int mid) {
    if (block) return; else block = true;
    intp.L2F(iid, mid); block = false;
  }

  public static void L2D(int iid, int mid) {
    if (block) return; else block = true;
    intp.L2D(iid, mid); block = false;
  }

  public static void F2I(int iid, int mid) {
    if (block) return; else block = true;
    intp.F2I(iid, mid); block = false;
  }

  public static void F2L(int iid, int mid) {
    if (block) return; else block = true;
    intp.F2L(iid, mid); block = false;
  }

  public static void F2D(int iid, int mid) {
    if (block) return; else block = true;
    intp.F2D(iid, mid); block = false;
  }

  public static void D2I(int iid, int mid) {
    if (block) return; else block = true;
    intp.D2I(iid, mid); block = false;
  }

  public static void D2L(int iid, int mid) {
    if (block) return; else block = true;
    intp.D2L(iid, mid); block = false;
  }

  public static void D2F(int iid, int mid) {
    if (block) return; else block = true;
    intp.D2F(iid, mid); block = false;
  }

  public static void I2B(int iid, int mid) {
    if (block) return; else block = true;
    intp.I2B(iid, mid); block = false;
  }

  public static void I2C(int iid, int mid) {
    if (block) return; else block = true;
    intp.I2C(iid, mid); block = false;
  }

  public static void I2S(int iid, int mid) {
    if (block) return; else block = true;
    intp.I2S(iid, mid); block = false;
  }

  public static void LCMP(int iid, int mid) {
    if (block) return; else block = true;
    intp.LCMP(iid, mid); block = false;
  }

  public static void FCMPL(int iid, int mid) {
    if (block) return; else block = true;
    intp.FCMPL(iid, mid); block = false;
  }

  public static void FCMPG(int iid, int mid) {
    if (block) return; else block = true;
    intp.FCMPG(iid, mid); block = false;
  }

  public static void DCMPL(int iid, int mid) {
    if (block) return; else block = true;
    intp.DCMPL(iid, mid); block = false;
  }

  public static void DCMPG(int iid, int mid) {
    if (block) return; else block = true;
    intp.DCMPG(iid, mid); block = false;
  }

  public static void IRETURN(int iid, int mid) {
    if (block) return; else block = true;
    intp.IRETURN(iid, mid); block = false;
  }

  public static void LRETURN(int iid, int mid) {
    if (block) return; else block = true;
    intp.LRETURN(iid, mid); block = false;
  }

  public static void FRETURN(int iid, int mid) {
    if (block) return; else block = true;
    intp.FRETURN(iid, mid); block = false;
  }

  public static void DRETURN(int iid, int mid) {
    if (block) return; else block = true;
    intp.DRETURN(iid, mid); block = false;
  }

  public static void ARETURN(int iid, int mid) {
    if (block) return; else block = true;
    intp.ARETURN(iid, mid); block = false;
  }

  public static void RETURN(int iid, int mid) {
    if (block) return; else block = true;
    intp.RETURN(iid, mid); block = false;
  }

  public static void ARRAYLENGTH(int iid, int mid) {
    if (block) return; else block = true;
    intp.ARRAYLENGTH(iid, mid); block = false;
  }

  public static void ATHROW(int iid, int mid) {
    if (block) return; else block = true;
    intp.ATHROW(iid, mid); block = false;
  }

  public static void MONITORENTER(int iid, int mid) {
    if (block) return; else block = true;
    intp.MONITORENTER(iid, mid); block = false;
  }

  public static void MONITOREXIT(int iid, int mid) {
    if (block) return; else block = true;
    intp.MONITOREXIT(iid, mid); block = false;
  }

  public static void GETVALUE_double(double v) {
    if (block) return; else block = true;
    intp.GETVALUE_double(v); block = false;
  }

  public static void GETVALUE_long(long v) {
    if (block) return; else block = true;
    intp.GETVALUE_long(v); block = false;
  }

  public static void GETVALUE_Object(Object v) {
    if (block) return; else block = true;
    intp.GETVALUE_Object(v); block = false;
  }

  public static void GETVALUE_boolean(boolean v) {
    if (block) return; else block = true;
    intp.GETVALUE_boolean(v); block = false;
  }

  public static void GETVALUE_byte(byte v) {
    if (block) return; else block = true;
    intp.GETVALUE_byte(v); block = false;
  }

  public static void GETVALUE_char(char v) {
    if (block) return; else block = true;
    intp.GETVALUE_char(v); block = false;
  }

  public static void GETVALUE_float(float v) {
    if (block) return; else block = true;
    intp.GETVALUE_float(v); block = false;
  }

  public static void GETVALUE_int(int v) {
    if (block) return; else block = true;
    intp.GETVALUE_int(v); block = false;
  }

  public static void GETVALUE_short(short v) {
    if (block) return; else block = true;
    intp.GETVALUE_short(v); block = false;
  }

  public static void GETVALUE_void() {
    if (block) return; else block = true;
    intp.GETVALUE_void(); block = false;
  }

  public static void INVOKEMETHOD_EXCEPTION() {
    if (block) return; else block = true;
    intp.INVOKEMETHOD_EXCEPTION(); block = false;
  }

  public static void INVOKEMETHOD_END() {
    if (block) return; else block = true;
    intp.INVOKEMETHOD_END(); block = false;
  }

  public static void SPECIAL(int i) {
    if (block) return; else block = true;
    intp.SPECIAL(i); block = false;
  }

  public static void MAKE_SYMBOLIC() {
    if (block) return; else block = true;
    intp.MAKE_SYMBOLIC(); block = false;
  }

  public static void flush() {
    intp.flush(); 
  }
}
