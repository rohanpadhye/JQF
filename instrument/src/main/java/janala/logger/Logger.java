package janala.logger;

public interface Logger {
  public void LDC(int iid, int mid, int c);

  public void LDC(int iid, int mid, long c);

  public void LDC(int iid, int mid, float c);

  public void LDC(int iid, int mid, double c);

  public void LDC(int iid, int mid, String c);

  public void LDC(int iid, int mid, Object c);

  public void IINC(int iid, int mid, int var, int increment);

  public void MULTIANEWARRAY(int iid, int mid, String desc, int dims);

  public void LOOKUPSWITCH(int iid, int mid, int dflt, int[] keys, int[] labels);

  public void TABLESWITCH(int iid, int mid, int min, int max, int dflt, int[] labels);

  public void IFEQ(int iid, int mid, int label);

  public void IFNE(int iid, int mid, int label);

  public void IFLT(int iid, int mid, int label);

  public void IFGE(int iid, int mid, int label);

  public void IFGT(int iid, int mid, int label);

  public void IFLE(int iid, int mid, int label);

  public void IF_ICMPEQ(int iid, int mid, int label);

  public void IF_ICMPNE(int iid, int mid, int label);

  public void IF_ICMPLT(int iid, int mid, int label);

  public void IF_ICMPGE(int iid, int mid, int label);

  public void IF_ICMPGT(int iid, int mid, int label);

  public void IF_ICMPLE(int iid, int mid, int label);

  public void IF_ACMPEQ(int iid, int mid, int label);

  public void IF_ACMPNE(int iid, int mid, int label);

  public void GOTO(int iid, int mid, int label);

  public void JSR(int iid, int mid, int label);

  public void IFNULL(int iid, int mid, int label);

  public void IFNONNULL(int iid, int mid, int label);

  public void INVOKEVIRTUAL(int iid, int mid, String owner, String name, String desc);

  public void INVOKESPECIAL(int iid, int mid, String owner, String name, String desc);

  public void INVOKESTATIC(int iid, int mid, String owner, String name, String desc);

  public void INVOKEINTERFACE(int iid, int mid, String owner, String name, String desc);

  public void HEAPLOAD(int iid, int mid, int objectId, String field);

  public void GETSTATIC(int iid, int mid, int cIdx, int fIdx, String desc);

  public void PUTSTATIC(int iid, int mid, int cIdx, int fIdx, String desc);

  public void GETFIELD(int iid, int mid, int cIdx, int fIdx, String desc);

  public void PUTFIELD(int iid, int mid, int cIdx, int fIdx, String desc);

  public void NEW(int iid, int mid, String type, int cIdx);

  public void ANEWARRAY(int iid, int mid, String type);

  public void CHECKCAST(int iid, int mid, String type);

  public void INSTANCEOF(int iid, int mid, String type);

  public void BIPUSH(int iid, int mid, int value);

  public void SIPUSH(int iid, int mid, int value);

  public void NEWARRAY(int iid, int mid);

  public void ILOAD(int iid, int mid, int var);

  public void LLOAD(int iid, int mid, int var);

  public void FLOAD(int iid, int mid, int var);

  public void DLOAD(int iid, int mid, int var);

  public void ALOAD(int iid, int mid, int var);

  public void ISTORE(int iid, int mid, int var);

  public void LSTORE(int iid, int mid, int var);

  public void FSTORE(int iid, int mid, int var);

  public void DSTORE(int iid, int mid, int var);

  public void ASTORE(int iid, int mid, int var);

  public void RET(int iid, int mid, int var);

  public void NOP(int iid, int mid);

  public void ACONST_NULL(int iid, int mid);

  public void ICONST_M1(int iid, int mid);

  public void ICONST_0(int iid, int mid);

  public void ICONST_1(int iid, int mid);

  public void ICONST_2(int iid, int mid);

  public void ICONST_3(int iid, int mid);

  public void ICONST_4(int iid, int mid);

  public void ICONST_5(int iid, int mid);

  public void LCONST_0(int iid, int mid);

  public void LCONST_1(int iid, int mid);

  public void FCONST_0(int iid, int mid);

  public void FCONST_1(int iid, int mid);

  public void FCONST_2(int iid, int mid);

  public void DCONST_0(int iid, int mid);

  public void DCONST_1(int iid, int mid);

  public void IALOAD(int iid, int mid);

  public void LALOAD(int iid, int mid);

  public void FALOAD(int iid, int mid);

  public void DALOAD(int iid, int mid);

  public void AALOAD(int iid, int mid);

  public void BALOAD(int iid, int mid);

  public void CALOAD(int iid, int mid);

  public void SALOAD(int iid, int mid);

  public void IASTORE(int iid, int mid);

  public void LASTORE(int iid, int mid);

  public void FASTORE(int iid, int mid);

  public void DASTORE(int iid, int mid);

  public void AASTORE(int iid, int mid);

  public void BASTORE(int iid, int mid);

  public void CASTORE(int iid, int mid);

  public void SASTORE(int iid, int mid);

  public void POP(int iid, int mid);

  public void POP2(int iid, int mid);

  public void DUP(int iid, int mid);

  public void DUP_X1(int iid, int mid);

  public void DUP_X2(int iid, int mid);

  public void DUP2(int iid, int mid);

  public void DUP2_X1(int iid, int mid);

  public void DUP2_X2(int iid, int mid);

  public void SWAP(int iid, int mid);

  public void IADD(int iid, int mid);

  public void LADD(int iid, int mid);

  public void FADD(int iid, int mid);

  public void DADD(int iid, int mid);

  public void ISUB(int iid, int mid);

  public void LSUB(int iid, int mid);

  public void FSUB(int iid, int mid);

  public void DSUB(int iid, int mid);

  public void IMUL(int iid, int mid);

  public void LMUL(int iid, int mid);

  public void FMUL(int iid, int mid);

  public void DMUL(int iid, int mid);

  public void IDIV(int iid, int mid);

  public void LDIV(int iid, int mid);

  public void FDIV(int iid, int mid);

  public void DDIV(int iid, int mid);

  public void IREM(int iid, int mid);

  public void LREM(int iid, int mid);

  public void FREM(int iid, int mid);

  public void DREM(int iid, int mid);

  public void INEG(int iid, int mid);

  public void LNEG(int iid, int mid);

  public void FNEG(int iid, int mid);

  public void DNEG(int iid, int mid);

  public void ISHL(int iid, int mid);

  public void LSHL(int iid, int mid);

  public void ISHR(int iid, int mid);

  public void LSHR(int iid, int mid);

  public void IUSHR(int iid, int mid);

  public void LUSHR(int iid, int mid);

  public void IAND(int iid, int mid);

  public void LAND(int iid, int mid);

  public void IOR(int iid, int mid);

  public void LOR(int iid, int mid);

  public void IXOR(int iid, int mid);

  public void LXOR(int iid, int mid);

  public void I2L(int iid, int mid);

  public void I2F(int iid, int mid);

  public void I2D(int iid, int mid);

  public void L2I(int iid, int mid);

  public void L2F(int iid, int mid);

  public void L2D(int iid, int mid);

  public void F2I(int iid, int mid);

  public void F2L(int iid, int mid);

  public void F2D(int iid, int mid);

  public void D2I(int iid, int mid);

  public void D2L(int iid, int mid);

  public void D2F(int iid, int mid);

  public void I2B(int iid, int mid);

  public void I2C(int iid, int mid);

  public void I2S(int iid, int mid);

  public void LCMP(int iid, int mid);

  public void FCMPL(int iid, int mid);

  public void FCMPG(int iid, int mid);

  public void DCMPL(int iid, int mid);

  public void DCMPG(int iid, int mid);

  public void IRETURN(int iid, int mid);

  public void LRETURN(int iid, int mid);

  public void FRETURN(int iid, int mid);

  public void DRETURN(int iid, int mid);

  public void ARETURN(int iid, int mid);

  public void RETURN(int iid, int mid);

  public void ARRAYLENGTH(int iid, int mid);

  public void ATHROW(int iid, int mid);

  public void MONITORENTER(int iid, int mid);

  public void MONITOREXIT(int iid, int mid);

  public void GETVALUE_double(double v);

  public void GETVALUE_long(long v);

  public void GETVALUE_Object(Object v);

  public void GETVALUE_boolean(boolean v);

  public void GETVALUE_byte(byte v);

  public void GETVALUE_char(char v);

  public void GETVALUE_float(float v);

  public void GETVALUE_int(int v);

  public void GETVALUE_short(short v);

  public void GETVALUE_void();

  public void METHOD_BEGIN(String owner, String name, String desc);

  public void METHOD_THROW();

  public void INVOKEMETHOD_EXCEPTION(Throwable t);

  public void INVOKEMETHOD_END();

  public void MAKE_SYMBOLIC();

  public void SPECIAL(int i);

  public void flush();
}
