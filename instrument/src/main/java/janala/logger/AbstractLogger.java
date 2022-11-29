package janala.logger;

import janala.logger.inst.*;

public abstract class AbstractLogger implements Logger {
  protected abstract void log(Instruction insn); 

  public void LDC(int iid, int mid, int c) {
    log(new LDC_int(iid, mid, c));
  }

  public void LDC(int iid, int mid, long c) {
    log(new LDC_long(iid, mid, c));
  }

  public void LDC(int iid, int mid, float c) {
    log(new LDC_float(iid, mid, c));
  }

  public void LDC(int iid, int mid, double c) {
    log(new LDC_double(iid, mid, c));
  }

  public void LDC(int iid, int mid, String c) {
    log(new LDC_String(iid, mid, c, System.identityHashCode(c)));
  }

  public void LDC(int iid, int mid, Object c) {
    log(new LDC_Object(iid, mid, System.identityHashCode(c)));
  }

  public void IINC(int iid, int mid, int var, int increment) {
    log(new IINC(iid, mid, var, increment));
  }

  public void MULTIANEWARRAY(int iid, int mid, String desc, int dims) {
    log(new MULTIANEWARRAY(iid, mid, desc, dims));
  }

  public void LOOKUPSWITCH(int iid, int mid, int dflt, int[] keys, int[] labels) {
    log(new LOOKUPSWITCH(iid, mid, dflt, keys, labels));
  }

  public void TABLESWITCH(int iid, int mid, int min, int max, int dflt, int[] labels) {
    log(new TABLESWITCH(iid, mid, min, max, dflt, labels));
  }

  public void IFEQ(int iid, int mid, int label) {
    log(new IFEQ(iid, mid, label));
  }

  public void IFNE(int iid, int mid, int label) {
    log(new IFNE(iid, mid, label));
  }

  public void IFLT(int iid, int mid, int label) {
    log(new IFLT(iid, mid, label));
  }

  public void IFGE(int iid, int mid, int label) {
    log(new IFGE(iid, mid, label));
  }

  public void IFGT(int iid, int mid, int label) {
    log(new IFGT(iid, mid, label));
  }

  public void IFLE(int iid, int mid, int label) {
    log(new IFLE(iid, mid, label));
  }

  public void IF_ICMPEQ(int iid, int mid, int label) {
    log(new IF_ICMPEQ(iid, mid, label));
  }

  public void IF_ICMPNE(int iid, int mid, int label) {
    log(new IF_ICMPNE(iid, mid, label));
  }

  public void IF_ICMPLT(int iid, int mid, int label) {
    log(new IF_ICMPLT(iid, mid, label));
  }

  public void IF_ICMPGE(int iid, int mid, int label) {
    log(new IF_ICMPGE(iid, mid, label));
  }

  public void IF_ICMPGT(int iid, int mid, int label) {
    log(new IF_ICMPGT(iid, mid, label));
  }

  public void IF_ICMPLE(int iid, int mid, int label) {
    log(new IF_ICMPLE(iid, mid, label));
  }

  public void IF_ACMPEQ(int iid, int mid, int label) {
    log(new IF_ACMPEQ(iid, mid, label));
  }

  public void IF_ACMPNE(int iid, int mid, int label) {
    log(new IF_ACMPNE(iid, mid, label));
  }

  public void GOTO(int iid, int mid, int label) {
    log(new GOTO(iid, mid, label));
  }

  public void JSR(int iid, int mid, int label) {
    log(new JSR(iid, mid, label));
  }

  public void IFNULL(int iid, int mid, int label) {
    log(new IFNULL(iid, mid, label));
  }

  public void IFNONNULL(int iid, int mid, int label) {
    log(new IFNONNULL(iid, mid, label));
  }

  public void INVOKEVIRTUAL(int iid, int mid, String owner, String name, String desc) {
    log(new INVOKEVIRTUAL(iid, mid, owner, name, desc));
  }

  public void INVOKESPECIAL(int iid, int mid, String owner, String name, String desc) {
    log(new INVOKESPECIAL(iid, mid, owner, name, desc));
  }

  public void INVOKESTATIC(int iid, int mid, String owner, String name, String desc) {
    log(new INVOKESTATIC(iid, mid, owner, name, desc));
  }

  public void INVOKEINTERFACE(int iid, int mid, String owner, String name, String desc) {
    log(new INVOKEINTERFACE(iid, mid, owner, name, desc));
  }

  public void GETSTATIC(int iid, int mid, int cIdx, int fIdx, String desc) {
    log(new GETSTATIC(iid, mid, cIdx, fIdx, desc));
  }

  public void PUTSTATIC(int iid, int mid, int cIdx, int fIdx, String desc) {
    log(new PUTSTATIC(iid, mid, cIdx, fIdx, desc));
  }

  public void GETFIELD(int iid, int mid, int cIdx, int fIdx, String desc) {
    log(new GETFIELD(iid, mid, cIdx, fIdx, desc));
  }

  public void PUTFIELD(int iid, int mid, int cIdx, int fIdx, String desc) {
    log(new PUTFIELD(iid, mid, cIdx, fIdx, desc));
  }

  public void HEAPLOAD(int iid, int mid, int objectId, String field) {
    log(new HEAPLOAD(iid, mid, objectId, field));
  }

  public void NEW(int iid, int mid, String type, int cIdx) {
    log(new NEW(iid, mid, type, cIdx));
  }

  public void ANEWARRAY(int iid, int mid, String type) {
    log(new ANEWARRAY(iid, mid, type));
  }

  public void CHECKCAST(int iid, int mid, String type) {
    log(new CHECKCAST(iid, mid, type));
  }

  public void INSTANCEOF(int iid, int mid, String type) {
    log(new INSTANCEOF(iid, mid, type));
  }

  public void BIPUSH(int iid, int mid, int value) {
    log(new BIPUSH(iid, mid, value));
  }

  public void SIPUSH(int iid, int mid, int value) {
    log(new SIPUSH(iid, mid, value));
  }

  public void NEWARRAY(int iid, int mid) {
    log(new NEWARRAY(iid, mid));
  }

  public void ILOAD(int iid, int mid, int var) {
    log(new ILOAD(iid, mid, var));
  }

  public void LLOAD(int iid, int mid, int var) {
    log(new LLOAD(iid, mid, var));
  }

  public void FLOAD(int iid, int mid, int var) {
    log(new FLOAD(iid, mid, var));
  }

  public void DLOAD(int iid, int mid, int var) {
    log(new DLOAD(iid, mid, var));
  }

  public void ALOAD(int iid, int mid, int var) {
    log(new ALOAD(iid, mid, var));
  }

  public void ISTORE(int iid, int mid, int var) {
    log(new ISTORE(iid, mid, var));
  }

  public void LSTORE(int iid, int mid, int var) {
    log(new LSTORE(iid, mid, var));
  }

  public void FSTORE(int iid, int mid, int var) {
    log(new FSTORE(iid, mid, var));
  }

  public void DSTORE(int iid, int mid, int var) {
    log(new DSTORE(iid, mid, var));
  }

  public void ASTORE(int iid, int mid, int var) {
    log(new ASTORE(iid, mid, var));
  }

  public void RET(int iid, int mid, int var) {
    log(new RET(iid, mid, var));
  }

  public void NOP(int iid, int mid) {
    log(new NOP(iid, mid));
  }

  public void ACONST_NULL(int iid, int mid) {
    log(new ACONST_NULL(iid, mid));
  }

  public void ICONST_M1(int iid, int mid) {
    log(new ICONST_M1(iid, mid));
  }

  public void ICONST_0(int iid, int mid) {
    log(new ICONST_0(iid, mid));
  }

  public void ICONST_1(int iid, int mid) {
    log(new ICONST_1(iid, mid));
  }

  public void ICONST_2(int iid, int mid) {
    log(new ICONST_2(iid, mid));
  }

  public void ICONST_3(int iid, int mid) {
    log(new ICONST_3(iid, mid));
  }

  public void ICONST_4(int iid, int mid) {
    log(new ICONST_4(iid, mid));
  }

  public void ICONST_5(int iid, int mid) {
    log(new ICONST_5(iid, mid));
  }

  public void LCONST_0(int iid, int mid) {
    log(new LCONST_0(iid, mid));
  }

  public void LCONST_1(int iid, int mid) {
    log(new LCONST_1(iid, mid));
  }

  public void FCONST_0(int iid, int mid) {
    log(new FCONST_0(iid, mid));
  }

  public void FCONST_1(int iid, int mid) {
    log(new FCONST_1(iid, mid));
  }

  public void FCONST_2(int iid, int mid) {
    log(new FCONST_2(iid, mid));
  }

  public void DCONST_0(int iid, int mid) {
    log(new DCONST_0(iid, mid));
  }

  public void DCONST_1(int iid, int mid) {
    log(new DCONST_1(iid, mid));
  }

  public void IALOAD(int iid, int mid) {
    log(new IALOAD(iid, mid));
  }

  public void LALOAD(int iid, int mid) {
    log(new LALOAD(iid, mid));
  }

  public void FALOAD(int iid, int mid) {
    log(new FALOAD(iid, mid));
  }

  public void DALOAD(int iid, int mid) {
    log(new DALOAD(iid, mid));
  }

  public void AALOAD(int iid, int mid) {
    log(new AALOAD(iid, mid));
  }

  public void BALOAD(int iid, int mid) {
    log(new BALOAD(iid, mid));
  }

  public void CALOAD(int iid, int mid) {
    log(new CALOAD(iid, mid));
  }

  public void SALOAD(int iid, int mid) {
    log(new SALOAD(iid, mid));
  }

  public void IASTORE(int iid, int mid) {
    log(new IASTORE(iid, mid));
  }

  public void LASTORE(int iid, int mid) {
    log(new LASTORE(iid, mid));
  }

  public void FASTORE(int iid, int mid) {
    log(new FASTORE(iid, mid));
  }

  public void DASTORE(int iid, int mid) {
    log(new DASTORE(iid, mid));
  }

  public void AASTORE(int iid, int mid) {
    log(new AASTORE(iid, mid));
  }

  public void BASTORE(int iid, int mid) {
    log(new BASTORE(iid, mid));
  }

  public void CASTORE(int iid, int mid) {
    log(new CASTORE(iid, mid));
  }

  public void SASTORE(int iid, int mid) {
    log(new SASTORE(iid, mid));
  }

  public void POP(int iid, int mid) {
    log(new POP(iid, mid));
  }

  public void POP2(int iid, int mid) {
    log(new POP2(iid, mid));
  }

  public void DUP(int iid, int mid) {
    log(new DUP(iid, mid));
  }

  public void DUP_X1(int iid, int mid) {
    log(new DUP_X1(iid, mid));
  }

  public void DUP_X2(int iid, int mid) {
    log(new DUP_X2(iid, mid));
  }

  public void DUP2(int iid, int mid) {
    log(new DUP2(iid, mid));
  }

  public void DUP2_X1(int iid, int mid) {
    log(new DUP2_X1(iid, mid));
  }

  public void DUP2_X2(int iid, int mid) {
    log(new DUP2_X2(iid, mid));
  }

  public void SWAP(int iid, int mid) {
    log(new SWAP(iid, mid));
  }

  public void IADD(int iid, int mid) {
    log(new IADD(iid, mid));
  }

  public void LADD(int iid, int mid) {
    log(new LADD(iid, mid));
  }

  public void FADD(int iid, int mid) {
    log(new FADD(iid, mid));
  }

  public void DADD(int iid, int mid) {
    log(new DADD(iid, mid));
  }

  public void ISUB(int iid, int mid) {
    log(new ISUB(iid, mid));
  }

  public void LSUB(int iid, int mid) {
    log(new LSUB(iid, mid));
  }

  public void FSUB(int iid, int mid) {
    log(new FSUB(iid, mid));
  }

  public void DSUB(int iid, int mid) {
    log(new DSUB(iid, mid));
  }

  public void IMUL(int iid, int mid) {
    log(new IMUL(iid, mid));
  }

  public void LMUL(int iid, int mid) {
    log(new LMUL(iid, mid));
  }

  public void FMUL(int iid, int mid) {
    log(new FMUL(iid, mid));
  }

  public void DMUL(int iid, int mid) {
    log(new DMUL(iid, mid));
  }

  public void IDIV(int iid, int mid) {
    log(new IDIV(iid, mid));
  }

  public void LDIV(int iid, int mid) {
    log(new LDIV(iid, mid));
  }

  public void FDIV(int iid, int mid) {
    log(new FDIV(iid, mid));
  }

  public void DDIV(int iid, int mid) {
    log(new DDIV(iid, mid));
  }

  public void IREM(int iid, int mid) {
    log(new IREM(iid, mid));
  }

  public void LREM(int iid, int mid) {
    log(new LREM(iid, mid));
  }

  public void FREM(int iid, int mid) {
    log(new FREM(iid, mid));
  }

  public void DREM(int iid, int mid) {
    log(new DREM(iid, mid));
  }

  public void INEG(int iid, int mid) {
    log(new INEG(iid, mid));
  }

  public void LNEG(int iid, int mid) {
    log(new LNEG(iid, mid));
  }

  public void FNEG(int iid, int mid) {
    log(new FNEG(iid, mid));
  }

  public void DNEG(int iid, int mid) {
    log(new DNEG(iid, mid));
  }

  public void ISHL(int iid, int mid) {
    log(new ISHL(iid, mid));
  }

  public void LSHL(int iid, int mid) {
    log(new LSHL(iid, mid));
  }

  public void ISHR(int iid, int mid) {
    log(new ISHR(iid, mid));
  }

  public void LSHR(int iid, int mid) {
    log(new LSHR(iid, mid));
  }

  public void IUSHR(int iid, int mid) {
    log(new IUSHR(iid, mid));
  }

  public void LUSHR(int iid, int mid) {
    log(new LUSHR(iid, mid));
  }

  public void IAND(int iid, int mid) {
    log(new IAND(iid, mid));
  }

  public void LAND(int iid, int mid) {
    log(new LAND(iid, mid));
  }

  public void IOR(int iid, int mid) {
    log(new IOR(iid, mid));
  }

  public void LOR(int iid, int mid) {
    log(new LOR(iid, mid));
  }

  public void IXOR(int iid, int mid) {
    log(new IXOR(iid, mid));
  }

  public void LXOR(int iid, int mid) {
    log(new LXOR(iid, mid));
  }

  public void I2L(int iid, int mid) {
    log(new I2L(iid, mid));
  }

  public void I2F(int iid, int mid) {
    log(new I2F(iid, mid));
  }

  public void I2D(int iid, int mid) {
    log(new I2D(iid, mid));
  }

  public void L2I(int iid, int mid) {
    log(new L2I(iid, mid));
  }

  public void L2F(int iid, int mid) {
    log(new L2F(iid, mid));
  }

  public void L2D(int iid, int mid) {
    log(new L2D(iid, mid));
  }

  public void F2I(int iid, int mid) {
    log(new F2I(iid, mid));
  }

  public void F2L(int iid, int mid) {
    log(new F2L(iid, mid));
  }

  public void F2D(int iid, int mid) {
    log(new F2D(iid, mid));
  }

  public void D2I(int iid, int mid) {
    log(new D2I(iid, mid));
  }

  public void D2L(int iid, int mid) {
    log(new D2L(iid, mid));
  }

  public void D2F(int iid, int mid) {
    log(new D2F(iid, mid));
  }

  public void I2B(int iid, int mid) {
    log(new I2B(iid, mid));
  }

  public void I2C(int iid, int mid) {
    log(new I2C(iid, mid));
  }

  public void I2S(int iid, int mid) {
    log(new I2S(iid, mid));
  }

  public void LCMP(int iid, int mid) {
    log(new LCMP(iid, mid));
  }

  public void FCMPL(int iid, int mid) {
    log(new FCMPL(iid, mid));
  }

  public void FCMPG(int iid, int mid) {
    log(new FCMPG(iid, mid));
  }

  public void DCMPL(int iid, int mid) {
    log(new DCMPL(iid, mid));
  }

  public void DCMPG(int iid, int mid) {
    log(new DCMPG(iid, mid));
  }

  public void IRETURN(int iid, int mid) {
    log(new IRETURN(iid, mid));
  }

  public void LRETURN(int iid, int mid) {
    log(new LRETURN(iid, mid));
  }

  public void FRETURN(int iid, int mid) {
    log(new FRETURN(iid, mid));
  }

  public void DRETURN(int iid, int mid) {
    log(new DRETURN(iid, mid));
  }

  public void ARETURN(int iid, int mid) {
    log(new ARETURN(iid, mid));
  }

  public void RETURN(int iid, int mid) {
    log(new RETURN(iid, mid));
  }

  public void ARRAYLENGTH(int iid, int mid) {
    log(new ARRAYLENGTH(iid, mid));
  }

  public void ATHROW(int iid, int mid) {
    log(new ATHROW(iid, mid));
  }

  public void MONITORENTER(int iid, int mid) {
    log(new MONITORENTER(iid, mid));
  }

  public void MONITOREXIT(int iid, int mid) {
    log(new MONITOREXIT(iid, mid));
  }

  public void GETVALUE_double(double v) {
    log(new GETVALUE_double(v));
  }

  public void GETVALUE_long(long v) {
    log(new GETVALUE_long(v));
  }

  public void GETVALUE_Object(Object v) {
    boolean isString = v instanceof String;
    log(new GETVALUE_Object(System.identityHashCode(v), isString ? ((String) v) : null, isString));
  }

  public void GETVALUE_boolean(boolean v) {
    log(new GETVALUE_boolean(v));
  }

  public void GETVALUE_byte(byte v) {
    log(new GETVALUE_byte(v));
  }

  public void GETVALUE_char(char v) {
    log(new GETVALUE_char(v));
  }

  public void GETVALUE_float(float v) {
    log(new GETVALUE_float(v));
  }

  public void GETVALUE_int(int v) {
    log(new GETVALUE_int(v));
  }

  public void GETVALUE_short(short v) {
    log(new GETVALUE_short(v));
  }

  public void GETVALUE_void() {
    log(new GETVALUE_void());
  }

  public void METHOD_BEGIN(String owner, String name, String desc) {
    log(new METHOD_BEGIN(owner, name, desc));
  }

  public void METHOD_BEGIN(String owner, String name, String desc, Object obj) {
    log(new METHOD_BEGIN(owner, name, desc, obj));
  }

  public void METHOD_THROW() {  log(new METHOD_THROW());  }

  public void INVOKEMETHOD_EXCEPTION(Throwable err) {
    log(new INVOKEMETHOD_EXCEPTION(err));
  }

  public void INVOKEMETHOD_END() {
    log(new INVOKEMETHOD_END());
  }

  public void MAKE_SYMBOLIC() {
    log(new MAKE_SYMBOLIC());
  }

  public void SPECIAL(int i) {
    log(new SPECIAL(i));
  }

  public void flush() {
    log(null);
  }

}
