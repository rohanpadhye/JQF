package janala.instrument;

import java.util.LinkedList;

import janala.logger.inst.SPECIAL;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class SnoopInstructionMethodAdapter extends MethodVisitor implements Opcodes {
  boolean isInit;
  boolean isSuperInitCalled;
  LinkedList<TryCatchBlock> tryCatchBlocks;
  boolean calledNew = false;
  Label methodBeginLabel = new Label();
  Label methodEndLabel = new Label();

  private final String className;
  private final String methodName;
  private final String descriptor;
  private final String superName;

  private final GlobalStateForInstrumentation instrumentationState;

  public SnoopInstructionMethodAdapter(MethodVisitor mv, String className,
      String methodName, String descriptor, String superName,
      GlobalStateForInstrumentation instrumentationState) {
    super(ASM5, mv);
    this.isInit = methodName.equals("<init>");
    this.isSuperInitCalled = false;
    this.className = className;
    this.methodName = methodName;
    this.descriptor = descriptor;
    this.superName = superName;
    tryCatchBlocks = new LinkedList<>();

    this.instrumentationState = instrumentationState;
  }

  @Override
  public void visitCode() {
    instrumentationState.incMid();
    mv.visitLdcInsn(className);
    mv.visitLdcInsn(methodName);
    mv.visitLdcInsn(descriptor);
    mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "METHOD_BEGIN", 
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
    if (isInit == false) {
      // For non-constructor methods, the outer try-catch blocks wraps around the entire code
      mv.visitLabel(methodBeginLabel);
    }
    mv.visitCode();

  }

  /** Push a value onto the stack. */
  private static void addBipushInsn(MethodVisitor mv, int val) {
    Utils.addBipushInsn(mv, val);
  }

  /** Add a GETVALUE call to synchronize the top stack with that of the symbolic stack. */
  private void addValueReadInsn(MethodVisitor mv, String desc, String methodNamePrefix) {
    Utils.addValueReadInsn(mv, desc, methodNamePrefix);
  }

  /** Add a special probe instruction. */
  private void addSpecialInsn(MethodVisitor mv, int val) {
    Utils.addSpecialInsn(mv, val);
  }

  private void addInsn(MethodVisitor mv, String insn, int opcode) {
    addBipushInsn(mv, instrumentationState.incAndGetId());
    addBipushInsn(mv, lastLineNumber);
    mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, insn, "(II)V", false);

    mv.visitInsn(opcode);
  }

  /** Add var insn and its instrumentation code. */
  private void addVarInsn(MethodVisitor mv, int var, String insn, int opcode) {
    addBipushInsn(mv, instrumentationState.incAndGetId());
    addBipushInsn(mv, lastLineNumber);
    addBipushInsn(mv, var);
    mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, insn, "(III)V", false);

    mv.visitVarInsn(opcode, var);
  }

  private void addTypeInsn(MethodVisitor mv, String type, int opcode, String name) {
    addBipushInsn(mv, instrumentationState.incAndGetId());
    addBipushInsn(mv, lastLineNumber);
    mv.visitLdcInsn(type);
    mv.visitMethodInsn(
      INVOKESTATIC, Config.instance.analysisClass, name, "(IILjava/lang/String;)V", false);
    mv.visitTypeInsn(opcode, type);
  }


  @Override
  public void visitInsn(int opcode) {
    switch (opcode) {/*
      case NOP:
        addInsn(mv, "NOP", opcode);
        break;
      case ACONST_NULL:
        addInsn(mv, "ACONST_NULL", opcode);
        break;
      case ICONST_M1:
        addInsn(mv, "ICONST_M1", opcode);
        break;
      case ICONST_0:
        addInsn(mv, "ICONST_0", opcode);
        break;
      case ICONST_1:
        addInsn(mv, "ICONST_1", opcode);
        break;
      case ICONST_2:
        addInsn(mv, "ICONST_2", opcode);
        break;
      case ICONST_3:
        addInsn(mv, "ICONST_3", opcode);
        break;
      case ICONST_4:
        addInsn(mv, "ICONST_4", opcode);
        break;
      case ICONST_5:
        addInsn(mv, "ICONST_5", opcode);
        break;
      case LCONST_0:
        addInsn(mv, "LCONST_0", opcode);
        break;
      case LCONST_1:
        addInsn(mv, "LCONST_1", opcode);
        break;
      case FCONST_0:
        addInsn(mv, "FCONST_0", opcode);
        break;
      case FCONST_1:
        addInsn(mv, "FCONST_1", opcode);
        break;
      case FCONST_2:
        addInsn(mv, "FCONST_2", opcode);
        break;
      case DCONST_0:
        addInsn(mv, "DCONST_0", opcode);
        break;
      case DCONST_1:
        addInsn(mv, "DCONST_1", opcode);
        break;
      case IALOAD:
        addInsn(mv, "IALOAD", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        addValueReadInsn(mv, "I", "GETVALUE_");
        break;
      case LALOAD:
        addInsn(mv, "LALOAD", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        addValueReadInsn(mv, "J", "GETVALUE_");
        break;
      case FALOAD:
        addInsn(mv, "FALOAD", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        addValueReadInsn(mv, "F", "GETVALUE_");
        break;
      case DALOAD:
        addInsn(mv, "DALOAD", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        addValueReadInsn(mv, "D", "GETVALUE_");
        break;
      case AALOAD:
        addInsn(mv, "AALOAD", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        addValueReadInsn(mv, "Ljava/lang/Object;", "GETVALUE_");
        break;
      case BALOAD:
        addInsn(mv, "BALOAD", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        addValueReadInsn(mv, "B", "GETVALUE_");
        break;
      case CALOAD:
        addInsn(mv, "CALOAD", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        addValueReadInsn(mv, "C", "GETVALUE_");
        break;
      case SALOAD:
        addInsn(mv, "SALOAD", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        addValueReadInsn(mv, "S", "GETVALUE_");
        break;
      case IASTORE:
        addInsn(mv, "IASTORE", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        break;
      case LASTORE:
        addInsn(mv, "LASTORE", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        break;
      case FASTORE:
        addInsn(mv, "FASTORE", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        break;
      case DASTORE:
        addInsn(mv, "DASTORE", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        break;
      case AASTORE:
        addInsn(mv, "AASTORE", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        break;
      case BASTORE:
        addInsn(mv, "BASTORE", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        break;
      case CASTORE:
        addInsn(mv, "CASTORE", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        break;
      case SASTORE:
        addInsn(mv, "SASTORE", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        break;
      case POP:
        addInsn(mv, "POP", opcode);
        break;
      case POP2:
        addInsn(mv, "POP2", opcode);
        break;
      case DUP:
        addInsn(mv, "DUP", opcode);
        break;
      case DUP_X1:
        addInsn(mv, "DUP_X1", opcode);
        break;
      case DUP_X2:
        addInsn(mv, "DUP_X2", opcode);
        break;
      case DUP2:
        addInsn(mv, "DUP2", opcode);
        break;
      case DUP2_X1:
        addInsn(mv, "DUP2_X1", opcode);
        break;
      case DUP2_X2:
        addInsn(mv, "DUP2_X2", opcode);
        break;
      case SWAP:
        addInsn(mv, "SWAP", opcode);
        break;
      case IADD:
        addInsn(mv, "IADD", opcode);
        break;
      case LADD:
        addInsn(mv, "LADD", opcode);
        break;
      case FADD:
        addInsn(mv, "FADD", opcode);
        break;
      case DADD:
        addInsn(mv, "DADD", opcode);
        break;
      case ISUB:
        addInsn(mv, "ISUB", opcode);
        break;
      case LSUB:
        addInsn(mv, "LSUB", opcode);
        break;
      case FSUB:
        addInsn(mv, "FSUB", opcode);
        break;
      case DSUB:
        addInsn(mv, "DSUB", opcode);
        break;
      case IMUL:
        addInsn(mv, "IMUL", opcode);
        break;
      case LMUL:
        addInsn(mv, "LMUL", opcode);
        break;
      case FMUL:
        addInsn(mv, "FMUL", opcode);
        break;
      case DMUL:
        addInsn(mv, "DMUL", opcode);
        break;
      case IDIV:
        addInsn(mv, "IDIV", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        break;
      case LDIV:
        addInsn(mv, "LDIV", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        break;
      case FDIV:
        addInsn(mv, "FDIV", opcode);
        break;
      case DDIV:
        addInsn(mv, "DDIV", opcode);
        break;
      case IREM:
        addInsn(mv, "IREM", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        break;
      case LREM:
        addInsn(mv, "LREM", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        break;
      case FREM:
        addInsn(mv, "FREM", opcode);
        break;
      case DREM:
        addInsn(mv, "DREM", opcode);
        break;
      case INEG:
        addInsn(mv, "INEG", opcode);
        break;
      case LNEG:
        addInsn(mv, "LNEG", opcode);
        break;
      case FNEG:
        addInsn(mv, "FNEG", opcode);
        break;
      case DNEG:
        addInsn(mv, "DNEG", opcode);
        break;
      case ISHL:
        addInsn(mv, "ISHL", opcode);
        break;
      case LSHL:
        addInsn(mv, "LSHL", opcode);
        break;
      case ISHR:
        addInsn(mv, "ISHR", opcode);
        break;
      case LSHR:
        addInsn(mv, "LSHR", opcode);
        break;
      case IUSHR:
        addInsn(mv, "IUSHR", opcode);
        break;
      case LUSHR:
        addInsn(mv, "LUSHR", opcode);
        break;
      case IAND:
        addInsn(mv, "IAND", opcode);
        break;
      case LAND:
        addInsn(mv, "LAND", opcode);
        break;
      case IOR:
        addInsn(mv, "IOR", opcode);
        break;
      case LOR:
        addInsn(mv, "LOR", opcode);
        break;
      case IXOR:
        addInsn(mv, "IXOR", opcode);
        break;
      case LXOR:
        addInsn(mv, "LXOR", opcode);
        break;
      case I2L:
        addInsn(mv, "I2L", opcode);
        break;
      case I2F:
        addInsn(mv, "I2F", opcode);
        break;
      case I2D:
        addInsn(mv, "I2D", opcode);
        break;
      case L2I:
        addInsn(mv, "L2I", opcode);
        break;
      case L2F:
        addInsn(mv, "L2F", opcode);
        break;
      case L2D:
        addInsn(mv, "L2D", opcode);
        break;
      case F2I:
        addInsn(mv, "F2I", opcode);
        break;
      case F2L:
        addInsn(mv, "F2L", opcode);
        break;
      case F2D:
        addInsn(mv, "F2D", opcode);
        break;
      case D2I:
        addInsn(mv, "D2I", opcode);
        break;
      case D2L:
        addInsn(mv, "D2L", opcode);
        break;
      case D2F:
        addInsn(mv, "D2F", opcode);
        break;
      case I2B:
        addInsn(mv, "I2B", opcode);
        break;
      case I2C:
        addInsn(mv, "I2C", opcode);
        break;
      case I2S:
        addInsn(mv, "I2S", opcode);
        break;
      case LCMP:
        addInsn(mv, "LCMP", opcode);
        break;
      case FCMPL:
        addInsn(mv, "FCMPL", opcode);
        break;
      case FCMPG:
        addInsn(mv, "FCMPG", opcode);
        break;
      case DCMPL:
        addInsn(mv, "DCMPL", opcode);
        break;
      case DCMPG:
        addInsn(mv, "DCMPG", opcode);
        break;*/
      case IRETURN:
        addInsn(mv, "IRETURN", opcode);
        break;
      case LRETURN:
        addInsn(mv, "LRETURN", opcode);
        break;
      case FRETURN:
        addInsn(mv, "FRETURN", opcode);
        break;
      case DRETURN:
        addInsn(mv, "DRETURN", opcode);
        break;
      case ARETURN:
        addInsn(mv, "ARETURN", opcode);
        break;
      case RETURN:
        addInsn(mv, "RETURN", opcode);
        break;
      /*case ARRAYLENGTH:
        addInsn(mv, "ARRAYLENGTH", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        addValueReadInsn(mv, "I", "GETVALUE_");
        break;
      case ATHROW:
        addInsn(mv, "ATHROW", opcode);
        break;
      case MONITORENTER:
        addInsn(mv, "MONITORENTER", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        break;
      case MONITOREXIT:
        addInsn(mv, "MONITOREXIT", opcode);
        addSpecialInsn(mv, 0); // for non-exceptional path
        break;*/


      case IALOAD:
      case LALOAD:
      case FALOAD:
      case DALOAD:
      case AALOAD:
      case BALOAD:
      case CALOAD:
      case SALOAD:
        if (Config.instance.instrumentHeapLoad) {
          mv.visitInsn(DUP2); // Duplicate array reference and index
          addBipushInsn(mv, instrumentationState.incAndGetId());
          addBipushInsn(mv, lastLineNumber);
          mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "HEAPLOAD2", "(Ljava/lang/Object;III)V", false);
        }
        mv.visitInsn(opcode); // Perform the actual operation
        break;
      default:
        mv.visitInsn(opcode); // Don't instrument other instructions
        //throw new RuntimeException("Unknown instruction opcode " + opcode);
    }
  }

  /*@Override
  public void visitVarInsn(int opcode, int var) {
    switch (opcode) {
      case ILOAD:
        addVarInsn(mv, var, "ILOAD", opcode);
        addValueReadInsn(mv, "I", "GETVALUE_");
        break;
      case LLOAD:
        addVarInsn(mv, var, "LLOAD", opcode);
        addValueReadInsn(mv, "J", "GETVALUE_");
        break;
      case FLOAD:
        addVarInsn(mv, var, "FLOAD", opcode);
        addValueReadInsn(mv, "F", "GETVALUE_");
        break;
      case DLOAD:
        addVarInsn(mv, var, "DLOAD", opcode);
        addValueReadInsn(mv, "D", "GETVALUE_");
        break;
      case ALOAD:
        addVarInsn(mv, var, "ALOAD", opcode);
        if (!(var == 0 && isInit && !isSuperInitCalled)) {
          addValueReadInsn(mv, "Ljava/lang/Object;", "GETVALUE_");
        }
        break;
      case ISTORE:
        addVarInsn(mv, var, "ISTORE", opcode);
        break;
      case LSTORE:
        addVarInsn(mv, var, "LSTORE", opcode);
        break;
      case FSTORE:
        addVarInsn(mv, var, "FSTORE", opcode);
        break;
      case DSTORE:
        addVarInsn(mv, var, "DSTORE", opcode);
        break;
      case ASTORE:
        addVarInsn(mv, var, "ASTORE", opcode);
        break;
      case RET:
        addVarInsn(mv, var, "RET", opcode);
        break;
      default:
        throw new RuntimeException("Unknown var insn");
    }
  }*/

  /*@Override
  public void visitIntInsn(int opcode, int operand) {
    addBipushInsn(mv, instrumentationState.incAndGetId());
    addBipushInsn(mv, lastLineNumber);
    switch (opcode) {
      case BIPUSH:
        addBipushInsn(mv, operand);
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "BIPUSH", "(III)V", false);
        break;
      case SIPUSH:
        addBipushInsn(mv, operand);
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "SIPUSH", "(III)V", false);
        break;
      case NEWARRAY:
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "NEWARRAY", "(II)V", false);
        mv.visitIntInsn(opcode, operand);
        addSpecialInsn(mv, 0); // for non-exceptional path
        return;
      default:
        throw new RuntimeException("Unknown int instruction opcode " + opcode);
    }
    mv.visitIntInsn(opcode, operand);
  }*/

  /*@Override
  public void visitTypeInsn(int opcode, String type) {
    switch (opcode) {
      case NEW:
        addBipushInsn(mv, instrumentationState.incAndGetId());
        addBipushInsn(mv, lastLineNumber);
        mv.visitLdcInsn(type);
        int cIdx = classNames.get(type);
        addBipushInsn(mv, cIdx);
        mv.visitMethodInsn(
            INVOKESTATIC, Config.instance.analysisClass, "NEW", "(IILjava/lang/String;I)V", false);
        mv.visitTypeInsn(opcode, type);
        addSpecialInsn(mv, 0); // for non-exceptional path

        calledNew = true;
        break;
      case ANEWARRAY:
        addTypeInsn(mv, type, opcode, "ANEWARRAY");
        addSpecialInsn(mv, 0); // for non-exceptional path
        break;
      case CHECKCAST:
        addTypeInsn(mv, type, opcode, "CHECKCAST");
        addSpecialInsn(mv, 0); // for non-exceptional path
        break;
      case INSTANCEOF:
        addTypeInsn(mv, type, opcode, "INSTANCEOF");
        addSpecialInsn(mv, 0); // for non-exceptional path
        addValueReadInsn(mv, "I", "GETVALUE_");
        break;
      default:
        throw new RuntimeException("Unknown type instruction opcode " + opcode);
    }
  }*/

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String desc) {
    /*
    addBipushInsn(mv, instrumentationState.incAndGetId());
    addBipushInsn(mv, lastLineNumber);
    int cIdx = classNames.get(owner);
    addBipushInsn(mv, cIdx);
    ObjectInfo tmp = classNames.get(cIdx);
    switch (opcode) {
      case GETSTATIC:
        int fIdx = tmp.getIdx(name, true);
        addBipushInsn(mv, fIdx);
        mv.visitLdcInsn(desc);

        mv.visitMethodInsn(
            INVOKESTATIC, Config.instance.analysisClass, "GETSTATIC", "(IIIILjava/lang/String;)V", false);

        mv.visitFieldInsn(opcode, owner, name, desc);
        addSpecialInsn(mv, 0); // for non-exceptional path
        addValueReadInsn(mv, desc, "GETVALUE_");
        break;
      case PUTSTATIC:
        fIdx = tmp.getIdx(name, true);
        addBipushInsn(mv, fIdx);
        mv.visitLdcInsn(desc);

        mv.visitMethodInsn(
            INVOKESTATIC, Config.instance.analysisClass, "PUTSTATIC", "(IIIILjava/lang/String;)V", false);
        mv.visitFieldInsn(opcode, owner, name, desc);
        addSpecialInsn(mv, 0); // for non-exceptional path
        break;
      case GETFIELD:
        fIdx = tmp.getIdx(name, false);
        addBipushInsn(mv, fIdx);
        mv.visitLdcInsn(desc);

        mv.visitMethodInsn(
            INVOKESTATIC, Config.instance.analysisClass, "GETFIELD", "(IIIILjava/lang/String;)V", false);
        mv.visitFieldInsn(opcode, owner, name, desc);
        addSpecialInsn(mv, 0); // for non-exceptional path
        addValueReadInsn(mv, desc, "GETVALUE_");
        break;
      case PUTFIELD:
        fIdx = tmp.getIdx(name, false);
        addBipushInsn(mv, fIdx);
        mv.visitLdcInsn(desc);

        mv.visitMethodInsn(
            INVOKESTATIC, Config.instance.analysisClass, "PUTFIELD", "(IIIILjava/lang/String;)V", false);
        mv.visitFieldInsn(opcode, owner, name, desc);
        addSpecialInsn(mv, 0); // for non-exceptional path
        break;
      default:
        throw new RuntimeException("Unknown field access opcode " + opcode);
    }
    */
    if (opcode == GETFIELD && Config.instance.instrumentHeapLoad) {
      mv.visitInsn(DUP); // Duplicate object reference
      mv.visitLdcInsn(owner + "#" + name);
      addBipushInsn(mv, instrumentationState.incAndGetId());
      addBipushInsn(mv, lastLineNumber);
      mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "HEAPLOAD1", "(Ljava/lang/Object;Ljava/lang/String;II)V", false);
    }
    mv.visitFieldInsn(opcode, owner, name, desc);
  }

  private String getMethodName(int opcode) {
    switch (opcode) {
      case INVOKESPECIAL:
        return "INVOKESPECIAL";
      case INVOKESTATIC:
        return "INVOKESTATIC";
      case INVOKEINTERFACE:
        return "INVOKEINTERFACE";
      case INVOKEVIRTUAL:
        return "INVOKEVIRTUAL";
      default:
        throw new RuntimeException("Unknown opcode for method");
    }
  }

  private void addMethodWithTryCatch(int opcode, String owner, String name, String desc, boolean itf) {
    addBipushInsn(mv, instrumentationState.incAndGetId());
    addBipushInsn(mv, lastLineNumber);

    mv.visitLdcInsn(owner);
    mv.visitLdcInsn(name);
    mv.visitLdcInsn(desc);
    mv.visitMethodInsn(
     INVOKESTATIC,
     Config.instance.analysisClass,
     getMethodName(opcode),
     "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);
      // Wrap the method call in a try-catch block
    Label begin = new Label();
    Label handler = new Label();
    Label end = new Label();

    tryCatchBlocks.addFirst(new TryCatchBlock(begin, handler, handler, null));

    mv.visitLabel(begin);
    mv.visitMethodInsn(opcode, owner, name, desc, itf);
    mv.visitJumpInsn(GOTO, end);

    mv.visitLabel(handler);
    mv.visitMethodInsn(
     INVOKESTATIC, Config.instance.analysisClass, "INVOKEMETHOD_EXCEPTION", "()V", false);
    mv.visitInsn(ATHROW);

    mv.visitLabel(end);
    mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "INVOKEMETHOD_END", "()V", false);

    addValueReadInsn(mv, desc, "GETVALUE_");
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
    if (opcode == INVOKESPECIAL && name.equals("<init>")) {



      if (isInit && isSuperInitCalled == false &&
              (owner.equals(className) || owner.equals(superName))) {
        // Constructor calls to <init> method of the same or super class.
        //
        // XXX: This is a hack. We assume that if we see an <init> to same or
        // super class, then it must be a super() or this() call. However,
        // there are counter-examples such as `public Foo() { super(new Foo()); }`,
        // which will cause broken class files. This comment is here as a forewarning
        // for when this situation is eventually encountered due to a bytecode
        // verification error due to stack-map frames not matching up.
        //
        // In this case, we do not wrap the method call in try catch block as
        // it uses uninitialized this object.
        isSuperInitCalled = true;


        // Register the call to <init>
        addSpecialInsn(mv, SPECIAL.CALLING_SUPER_OR_THIS); // for true path
        addBipushInsn(mv, instrumentationState.incAndGetId());
        addBipushInsn(mv, lastLineNumber);
        mv.visitLdcInsn(owner);
        mv.visitLdcInsn(name);
        mv.visitLdcInsn(desc);
        mv.visitMethodInsn(
                INVOKESTATIC,
                Config.instance.analysisClass,
                getMethodName(opcode),
                "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", false);


        // Call <init>
        mv.visitMethodInsn(opcode, owner, name, desc, itf);
        if (calledNew) {
          calledNew = false;
        }

        // Mark end of <init>
        // Note: If <init> throws an exception, we do not log it, due to JVM restrictions;
        //   this must be inferred from the logs somehow
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "INVOKEMETHOD_END", "()V", false);



        // Handle super() for Thread.<init> specially
        // TODO: Handle direct calls to new Thread() without subclassing
        if (owner.equals("java/lang/Thread")) {
          mv.visitVarInsn(ALOAD, 0);
          mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "REGISTER_THREAD", "(Ljava/lang/Thread;)V", false);
        }

        // The outer try-catch starts after the call to super()
        mv.visitLabel(methodBeginLabel);

      } else {
        addMethodWithTryCatch(opcode, owner, name, desc, itf);
        if (calledNew) {
          calledNew = false;
          addValueReadInsn(mv, "Ljava/lang/Object;", "GETVALUE_");
        }
      }


    } else {
      addMethodWithTryCatch(opcode, owner, name, desc, itf);
    }
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    int iid3;
    addBipushInsn(mv, iid3 = instrumentationState.incAndGetId());
    addBipushInsn(mv, lastLineNumber);
    addBipushInsn(mv, getLabelNum(label));
    switch (opcode) {
      case IFEQ:
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "IFEQ", "(III)V", false);
        mv.visitJumpInsn(opcode, label);
        addSpecialInsn(mv, 1); // for true path
        break;
      case IFNE:
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "IFNE", "(III)V", false);
        mv.visitJumpInsn(opcode, label);
        addSpecialInsn(mv, 1); // for true path
        break;
      case IFLT:
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "IFLT", "(III)V", false);
        mv.visitJumpInsn(opcode, label);
        addSpecialInsn(mv, 1); // for true path
        break;
      case IFGE:
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "IFGE", "(III)V", false);
        mv.visitJumpInsn(opcode, label);
        addSpecialInsn(mv, 1); // for true path
        break;
      case IFGT:
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "IFGT", "(III)V", false);
        mv.visitJumpInsn(opcode, label);
        addSpecialInsn(mv, 1); // for true path
        break;
      case IFLE:
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "IFLE", "(III)V", false);
        mv.visitJumpInsn(opcode, label);
        addSpecialInsn(mv, 1); // for true path
        break;
      case IF_ICMPEQ:
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "IF_ICMPEQ", "(III)V", false);
        mv.visitJumpInsn(opcode, label);
        addSpecialInsn(mv, 1); // for true path
        break;
      case IF_ICMPNE:
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "IF_ICMPNE", "(III)V", false);
        mv.visitJumpInsn(opcode, label);
        addSpecialInsn(mv, 1); // for true path
        break;
      case IF_ICMPLT:
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "IF_ICMPLT", "(III)V", false);
        mv.visitJumpInsn(opcode, label);
        addSpecialInsn(mv, 1); // for true path
        break;
      case IF_ICMPGE:
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "IF_ICMPGE", "(III)V", false);
        mv.visitJumpInsn(opcode, label);
        addSpecialInsn(mv, 1); // for true path
        break;
      case IF_ICMPGT:
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "IF_ICMPGT", "(III)V", false);
        mv.visitJumpInsn(opcode, label);
        addSpecialInsn(mv, 1); // for true path
        break;
      case IF_ICMPLE:
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "IF_ICMPLE", "(III)V", false);
        mv.visitJumpInsn(opcode, label);
        addSpecialInsn(mv, 1); // for true path
        break;
      case IF_ACMPEQ:
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "IF_ACMPEQ", "(III)V", false);
        mv.visitJumpInsn(opcode, label);
        addSpecialInsn(mv, 1); // for true path
        break;
      case IF_ACMPNE:
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "IF_ACMPNE", "(III)V", false);
        mv.visitJumpInsn(opcode, label);
        addSpecialInsn(mv, 1); // for true path
        break;
      case GOTO:
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "GOTO", "(III)V", false);
        mv.visitJumpInsn(opcode, label);
        break;
      case JSR:
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "JSR", "(III)V", false);
        mv.visitJumpInsn(opcode, label);
        break;
      case IFNULL:
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "IFNULL", "(III)V", false);
        mv.visitJumpInsn(opcode, label);
        addSpecialInsn(mv, 1); // for true path
        break;
      case IFNONNULL:
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "IFNONNULL", "(III)V", false);
        mv.visitJumpInsn(opcode, label);
        addSpecialInsn(mv, 1); // for true path
        break;
      default:
        throw new RuntimeException("Unknown jump opcode " + opcode);
    }
  }

  /*@Override
  public void visitLdcInsn(Object cst) {
    addBipushInsn(mv, instrumentationState.incAndGetId());
    addBipushInsn(mv, lastLineNumber);
    mv.visitLdcInsn(cst);
    if (cst instanceof Integer) {
      mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "LDC", "(III)V", false);
    } else if (cst instanceof Long) {
      mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "LDC", "(IIJ)V", false);
    } else if (cst instanceof Float) {
      mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "LDC", "(IIF)V", false);
    } else if (cst instanceof Double) {
      mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "LDC", "(IID)V", false);
    } else if (cst instanceof String) {
      mv.visitMethodInsn(
          INVOKESTATIC, Config.instance.analysisClass, "LDC", "(IILjava/lang/String;)V", false);
    } else {
      mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, 
          "LDC", "(IILjava/lang/Object;)V", false);
    }
    mv.visitLdcInsn(cst);
  }*/

  /*@Override
  public void visitIincInsn(int var, int increment) {
    addBipushInsn(mv, instrumentationState.incAndGetId());
    addBipushInsn(mv, lastLineNumber);
    addBipushInsn(mv, var);
    addBipushInsn(mv, increment);
    mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "IINC", "(IIII)V", false);
    mv.visitIincInsn(var, increment);
  }*/

  private Integer lastLineNumber = 0;

  @Override
  public void visitLineNumber(int lineNumber, Label label) {
    lastLineNumber = lineNumber;
  }


  private int getLabelNum(Label label) {
    return System.identityHashCode(label);
  }





  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    // Save operand value
    addValueReadInsn(mv, "I", "GETVALUE_");
    // Log switch instruction
    addBipushInsn(mv, instrumentationState.incAndGetId());
    addBipushInsn(mv, lastLineNumber);
    addBipushInsn(mv, min);
    addBipushInsn(mv, max);
    addBipushInsn(mv, getLabelNum(dflt));

    addBipushInsn(mv, labels.length);
    mv.visitIntInsn(NEWARRAY, T_INT);
    for (int i = 0; i < labels.length; i++) {
      mv.visitInsn(DUP);
      addBipushInsn(mv, i);
      addBipushInsn(mv, getLabelNum(labels[i]));
      mv.visitInsn(IASTORE);
    }

    mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "TABLESWITCH", "(IIIII[I)V", false);
    mv.visitTableSwitchInsn(min, max, dflt, labels);
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    // Save operand value
    addValueReadInsn(mv, "I", "GETVALUE_");
    // Log switch instruction
    addBipushInsn(mv, instrumentationState.incAndGetId());
    addBipushInsn(mv, lastLineNumber);
    addBipushInsn(mv, getLabelNum(dflt));

    addBipushInsn(mv, keys.length);
    mv.visitIntInsn(NEWARRAY, T_INT);
    for (int i = 0; i < keys.length; i++) {
      mv.visitInsn(DUP);
      addBipushInsn(mv, i);
      addBipushInsn(mv, keys[i]);
      mv.visitInsn(IASTORE);
    }

    addBipushInsn(mv, labels.length);
    mv.visitIntInsn(NEWARRAY, T_INT);
    for (int i = 0; i < labels.length; i++) {
      mv.visitInsn(DUP);
      addBipushInsn(mv, i);
      addBipushInsn(mv, getLabelNum(labels[i]));
      mv.visitInsn(IASTORE);
    }

    mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "LOOKUPSWITCH", "(III[I[I)V", false);
    mv.visitLookupSwitchInsn(dflt, keys, labels);
  }

  /*@Override
  public void visitMultiANewArrayInsn(String desc, int dims) {
    addBipushInsn(mv, instrumentationState.incAndGetId());
    addBipushInsn(mv, lastLineNumber);
    mv.visitLdcInsn(desc);
    addBipushInsn(mv, dims);
    mv.visitMethodInsn(
        INVOKESTATIC, Config.instance.analysisClass, "MULTIANEWARRAY", "(IILjava/lang/String;I)V", false);
    mv.visitMultiANewArrayInsn(desc, dims);
    addSpecialInsn(mv, 0); // for non-exceptional path
  }*/

  @Override
  public void visitMaxs(int maxStack, int maxLocals) {
    // Wrap entire method body in a try-catch all; the methodBeginLabel is already added in visitCode()
    tryCatchBlocks.addLast(new TryCatchBlock(methodBeginLabel, methodEndLabel, methodEndLabel, null));

    mv.visitLabel(methodEndLabel);
    mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "METHOD_THROW", "()V", false);
    mv.visitInsn(ATHROW);


    for (TryCatchBlock b : tryCatchBlocks) {
      b.visit(mv);
    }
    mv.visitMaxs(
        maxStack + 8,
        maxLocals);



  }

  @Override
  public void visitTryCatchBlock(Label label, Label label1, Label label2, String s) {
    tryCatchBlocks.addLast(new TryCatchBlock(label, label1, label2, s));
  }
}
