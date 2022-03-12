package janala.instrument;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class FastCoverageMethodAdapter extends MethodVisitor implements Opcodes {
  boolean isInit;
  boolean isSuperInitCalled; // Used to keep track of calls to super()/this() in <init>()
  int newStack = 0; // Used to keep-track of NEW instructions in <init>()

  private final String className;
  private final String superName;

  private final GlobalStateForInstrumentation instrumentationState;

  private final int methodIID;
  public FastCoverageMethodAdapter(MethodVisitor mv, String className,
                                   String methodName, String descriptor, String superName,
                                   GlobalStateForInstrumentation instrumentationState) {
    super(ASM8, mv);
    this.isInit = methodName.equals("<init>");
    this.isSuperInitCalled = false;
    this.className = className;
    this.superName = superName;

    this.instrumentationState = instrumentationState;
    this.methodIID = instrumentationState.incAndGetFastCoverageId();
  }

  /** Push a value onto the stack. */
  private static void addBipushInsn(MethodVisitor mv, int val) {
    Utils.addBipushInsn(mv, val);
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
    int iid = instrumentationState.incAndGetFastCoverageId();
    addBipushInsn(mv, iid);
    mv.visitInsn(ICONST_0);
    mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "LOGJUMP", "(II)V", false);

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
      } else {
        // Call to <init> but not a super() or this(). Must have occurred after a NEW.
        // This is an outer constructor call, so reduce the NEW stack
        if (isInit) {
          newStack--;
          assert newStack >= 0;
        }
      }
    }
    mv.visitMethodInsn(opcode, owner, name, desc, itf);
  }

  @Override
  public void visitCode() {
    super.visitCode();
    addBipushInsn(mv, methodIID);
    mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "LOGMETHODBEGIN", "(I)V", false);
  }

  private void addConditionalJumpInstrumentation(int opcode, Label finalBranchTarget,
                                                 String instMethodName, String instMethodDesc) {
    int iid = instrumentationState.incAndGetFastCoverageId();
    instrumentationState.incAndGetFastCoverageId(); //reserve another counter for the other side of this branch

    Label intermediateBranchTarget = new Label();
    Label fallthrough = new Label();

    // Perform the original jump, but branch to intermediate label
    mv.visitJumpInsn(opcode, intermediateBranchTarget);
    // If we did not jump, skip to the fallthrough
    mv.visitJumpInsn(GOTO, fallthrough);

    // Now instrument the branch target
    mv.visitLabel(intermediateBranchTarget);
    addBipushInsn(mv, iid);
    addBipushInsn(mv, 1); // Mark branch as taken
    mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, instMethodName, instMethodDesc, false);
    mv.visitJumpInsn(GOTO, finalBranchTarget); // Go to actual branch target

    // Now instrument the fall through
    mv.visitLabel(fallthrough);
    addBipushInsn(mv, iid);
    addBipushInsn(mv, 0); // Mark branch as not taken
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
      case IFNE:
      case IFLT:
      case IFGE:
      case IFGT:
      case IFLE:
      case IF_ICMPEQ:
      case IF_ICMPNE:
      case IF_ICMPLT:
      case IF_ICMPGE:
      case IF_ICMPGT:
      case IF_ICMPLE:
      case IF_ACMPEQ:
      case IF_ACMPNE:
      case IFNULL:
      case IFNONNULL:
        addConditionalJumpInstrumentation(opcode, label,  "LOGJUMP", "(II)V");
        break;
      case GOTO:
      case JSR:
        mv.visitJumpInsn(opcode, label);
        break;
      default:
        throw new RuntimeException("Unknown jump opcode " + opcode);
    }
  }

  @Override
  public void visitInsn(int opcode) {
    switch (opcode) {
      case IRETURN:
      case LRETURN:
      case FRETURN:
      case DRETURN:
      case ARETURN:
      case RETURN:
        addBipushInsn(mv, methodIID);
        mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "LOGMETHODEND", "(I)V", false);
    }
    super.visitInsn(opcode);
  }

  private Integer lastLineNumber = 0;

  @Override
  public void visitLineNumber(int lineNumber, Label label) {
    lastLineNumber = lineNumber;
    mv.visitLineNumber(lineNumber, label);
  }


  private int getLabelNum(Label label) {
    return System.identityHashCode(label);
  }





  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
    // Save operand value
    //addValueReadInsn(mv, "I", "GETVALUE_");
    mv.visitInsn(Opcodes.DUP);
    // Log switch instruction
    addBipushInsn(mv, instrumentationState.incAndGetFastCoverageId());
    addBipushInsn(mv, min);
    addBipushInsn(mv, max);
    addBipushInsn(mv, getLabelNum(dflt));

    for (int i = 0; i < labels.length; i++) {
      //create a coverage probe for each of the arms, we'll refer to it by offset
      instrumentationState.incAndGetFastCoverageId();
    }


    //create a coverage probe for the default case
    instrumentationState.incAndGetFastCoverageId();
    mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "LOGTABLESWITCH", "(IIIII)V", false);
    mv.visitTableSwitchInsn(min, max, dflt, labels);
  }


  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    // Save operand value
    mv.visitInsn(Opcodes.DUP);

    // Log switch instruction
    addBipushInsn(mv, instrumentationState.incAndGetFastCoverageId());
    addBipushInsn(mv, getLabelNum(dflt));

    addBipushInsn(mv, keys.length);
    mv.visitIntInsn(NEWARRAY, T_INT);
    for (int i = 0; i < keys.length; i++) {
      mv.visitInsn(DUP);
      addBipushInsn(mv, i);
      addBipushInsn(mv, keys[i]);
      mv.visitInsn(IASTORE);
      //create a coverage probe for each of the arms, we'll refer to it by offset
      instrumentationState.incAndGetFastCoverageId();
    }


    //create a coverage probe for the default case
    instrumentationState.incAndGetFastCoverageId();
    mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, "LOGLOOKUPSWITCH", "(III[I)V", false);
    mv.visitLookupSwitchInsn(dflt, keys, labels);
  }

  @Override
  public void visitMaxs(int maxStack, int maxLocals) {
    //Allow ASM to calculate the correct maxStack by passing '0' as the maximum stack value.
    mv.visitMaxs(0, maxLocals);
  }
}
