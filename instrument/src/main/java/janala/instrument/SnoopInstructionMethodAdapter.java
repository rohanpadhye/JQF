package janala.instrument;

import java.util.LinkedList;

import janala.logger.inst.SPECIAL;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class SnoopInstructionMethodAdapter extends MethodVisitor implements Opcodes {
  boolean isInit;
  boolean isSuperInitCalled; // Used to keep track of calls to super()/this() in <init>()
  int newStack = 0; // Used to keep-track of NEW instructions in <init>()
  LinkedList<TryCatchBlock> tryCatchBlocks;
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

  @Override
  public void visitIntInsn(int opcode, int operand) {
    switch (opcode) {
      /*
      case BIPUSH:
        addBipushInsn(mv, operand);
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "BIPUSH", "(III)V", false);
        break;
      case SIPUSH:
        addBipushInsn(mv, operand);
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "SIPUSH", "(III)V", false);
        break;
       */
      case NEWARRAY:
        if (Config.instance.instrumentAlloc) {
          // First, log the array size
          addValueReadInsn(mv, "I", "GETVALUE_");
          // Then, log the NEWARRAY instruction
          addBipushInsn(mv, instrumentationState.incAndGetId());
          addBipushInsn(mv, lastLineNumber);
          mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "NEWARRAY", "(II)V", false);
        }
        break;
      default:
    }
    mv.visitIntInsn(opcode, operand);
  }

  @Override
  public void visitTypeInsn(int opcode, String type) {
    switch (opcode) {
      case NEW:
        if (Config.instance.instrumentAlloc) {
          // Log the NEW instruction
          addBipushInsn(mv, instrumentationState.incAndGetId());
          addBipushInsn(mv, lastLineNumber);
          mv.visitLdcInsn(type);
          mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "NEW", "(IILjava/lang/String;)V", false);
        }

        if (isInit) newStack++; // Used in <init>; see: #visitMethodInsn

        break;
      case ANEWARRAY:
        if (Config.instance.instrumentAlloc) {
          // First, log the array size
          addValueReadInsn(mv, "I", "GETVALUE_");
          // Then, log the ANEWARRAY instruction
          addBipushInsn(mv, instrumentationState.incAndGetId());
          addBipushInsn(mv, lastLineNumber);
          mv.visitLdcInsn(type);
          mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "ANEWARRAY", "(IILjava/lang/String;)V", false);
        }
        break;
      /*
      case CHECKCAST:
        addTypeInsn(mv, type, opcode, "CHECKCAST");
        addSpecialInsn(mv, 0); // for non-exceptional path
        break;
      case INSTANCEOF:
        addTypeInsn(mv, type, opcode, "INSTANCEOF");
        addSpecialInsn(mv, 0); // for non-exceptional path
        addValueReadInsn(mv, "I", "GETVALUE_");
        break;
      */
      default:
    }
    mv.visitTypeInsn(opcode, type);
  }

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

    // addValueReadInsn(mv, desc, "GETVALUE_");
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
    if (opcode == INVOKESPECIAL && name.equals("<init>")) {


      // The first call to <init> within a constructor (`isInit`) on the same or super class,
      // which is not associated with a NEW instruction (`newStack` == 0),
      // will be considered as an invocation of super()/this().

      if (isInit && isSuperInitCalled == false && newStack == 0 &&
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

        // Mark end of <init>
        // Note: If <init> throws an exception, we do not log it, due to JVM restrictions;
        //   this must be inferred from the logs somehow
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "INVOKEMETHOD_END", "()V", false);



        // Handle super() for Thread.<init> specially
        if (owner.equals("java/lang/Thread")) {
          mv.visitVarInsn(ALOAD, 0);
          mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "REGISTER_THREAD", "(Ljava/lang/Thread;)V", false);
        }

        // The outer try-catch starts after the call to super()
        mv.visitLabel(methodBeginLabel);

      } else {
        // Call to <init> but not a super() or this(). Must have occurred after a NEW.
        addMethodWithTryCatch(opcode, owner, name, desc, itf);

        // This is an outer constructor call, so reduce the NEW stack
        if (isInit) {
          newStack--;
          assert newStack >= 0;
        }

        // Handle direct calls to new Thread() without subclassing
        if (owner.equals("java/lang/Thread")) {
          // Assumming the NEW-DUP-INIT pattern, the top-of-stack after the <init>() invocation
          // should be the reference to the just-constructed object
          mv.visitInsn(DUP);
          mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "REGISTER_THREAD", "(Ljava/lang/Thread;)V", false);
        }
      }



    } else { // Call to non-constructor method

      // Specially handle methods like String.charAt for HEAPLOAD
      // since we do not instrument java.lang.String and friends.
      if (Config.instance.instrumentHeapLoad &&
              (name.equals("charAt") || name.equals("codePointAt")) &&
              (owner.equals("java/lang/String") || owner.equals("java/lang/CharSequence"))) {
        mv.visitInsn(DUP2); // Duplicate object reference and index
        addBipushInsn(mv, instrumentationState.incAndGetId());
        addBipushInsn(mv, lastLineNumber);
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "HEAPLOAD2", "(Ljava/lang/Object;III)V", false);
      }
      addMethodWithTryCatch(opcode, owner, name, desc, itf);
    }
  }

  private void addConditionalJumpInstrumentation(int opcode, Label finalBranchTarget,
                                                 String instMethodName, String instMethodDesc) {
    int iid = instrumentationState.incAndGetId();
    Label intermediateBranchTarget = new Label();
    Label fallthrough = new Label();

    // Perform the original jump, but branch to intermediate label
    mv.visitJumpInsn(opcode, intermediateBranchTarget);
    // If we did not jump, skip to the fallthrough
    mv.visitJumpInsn(GOTO, fallthrough);

    // Now instrument the branch target
    mv.visitLabel(intermediateBranchTarget);
    addBipushInsn(mv, 1); // Mark branch as taken
    addValueReadInsn(mv, "Z", "GETVALUE_"); // Send value to logger (Z for boolean)
    mv.visitInsn(POP);
    addBipushInsn(mv, iid);
    addBipushInsn(mv, lastLineNumber);
    addBipushInsn(mv, getLabelNum(finalBranchTarget));
    mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, instMethodName, instMethodDesc, false);
    mv.visitJumpInsn(GOTO, finalBranchTarget); // Go to actual branch target

    // Now instrument the fall through
    mv.visitLabel(fallthrough);
    addBipushInsn(mv, 0); // Mark branch as not taken
    addValueReadInsn(mv, "Z", "GETVALUE_"); // Send value to logger (Z for boolean)
    mv.visitInsn(POP);
    addBipushInsn(mv, iid);
    addBipushInsn(mv, lastLineNumber);
    addBipushInsn(mv, getLabelNum(finalBranchTarget));
    mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, instMethodName, instMethodDesc, false);

    // continue with fall-through code visiting
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    if (isInit && !isSuperInitCalled) {
      // Jumps in a constructor before super() or this() mess up the analysis
      throw new RuntimeException("Cannot handle jumps before super/this");
    }

    switch (opcode) {
      case IFEQ:
        addConditionalJumpInstrumentation(opcode, label, "IFEQ", "(III)V");
        break;
      case IFNE:
        addConditionalJumpInstrumentation(opcode, label, "IFNE", "(III)V");
        break;
      case IFLT:
        addConditionalJumpInstrumentation(opcode, label,  "IFLT", "(III)V");
        break;
      case IFGE:
        addConditionalJumpInstrumentation(opcode, label,  "IFGE", "(III)V");
        break;
      case IFGT:
        addConditionalJumpInstrumentation(opcode, label,  "IFGT", "(III)V");
        break;
      case IFLE:
        addConditionalJumpInstrumentation(opcode, label,  "IFLE", "(III)V");
        break;
      case IF_ICMPEQ:
        addConditionalJumpInstrumentation(opcode, label,  "IF_ICMPEQ", "(III)V");
        break;
      case IF_ICMPNE:
        addConditionalJumpInstrumentation(opcode, label,  "IF_ICMPNE", "(III)V");
        break;
      case IF_ICMPLT:
        addConditionalJumpInstrumentation(opcode, label,  "IF_ICMPLT", "(III)V");
        break;
      case IF_ICMPGE:
        addConditionalJumpInstrumentation(opcode, label,  "IF_ICMPGE", "(III)V");
        break;
      case IF_ICMPGT:
        addConditionalJumpInstrumentation(opcode, label,  "IF_ICMPGT", "(III)V");
        break;
      case IF_ICMPLE:
        addConditionalJumpInstrumentation(opcode, label,  "IF_ICMPLE", "(III)V");
        break;
      case IF_ACMPEQ:
        addConditionalJumpInstrumentation(opcode, label,  "IF_ACMPEQ", "(III)V");
        break;
      case IF_ACMPNE:
        addConditionalJumpInstrumentation(opcode, label,  "IF_ACMPNE", "(III)V");
        break;
      case GOTO:
        mv.visitJumpInsn(opcode, label);
        break;
      case JSR:
        mv.visitJumpInsn(opcode, label);
        break;
      case IFNULL:
        addConditionalJumpInstrumentation(opcode, label,  "IFNULL", "(III)V");
        break;
      case IFNONNULL:
        addConditionalJumpInstrumentation(opcode, label,  "IFNONNULL", "(III)V");
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
