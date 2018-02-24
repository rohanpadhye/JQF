package janala.instrument;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.Opcodes;

public class Utils implements Opcodes {
  public static void addBipushInsn(MethodVisitor mv, int val) {
    switch (val) {
      case 0:
        mv.visitInsn(ICONST_0);
        break;
      case 1:
        mv.visitInsn(ICONST_1);
        break;
      case 2:
        mv.visitInsn(ICONST_2);
        break;
      case 3:
        mv.visitInsn(ICONST_3);
        break;
      case 4:
        mv.visitInsn(ICONST_4);
        break;
      case 5:
        mv.visitInsn(ICONST_5);
        break;
      default:
        mv.visitLdcInsn(val);
        break;
    }
  }

  public static void addSpecialInsn(MethodVisitor mv, int val) {
    addBipushInsn(mv, val);
    mv.visitMethodInsn(INVOKESTATIC, Config.instance.analysisClass, 
      "SPECIAL", "(I)V", false);
  }

  /* Adds a set to code to read the given type from the top of the concrete stack
      and invoke GETVALUE method of the analysis class. */
  public static void addValueReadInsn(MethodVisitor mv, String desc, String methodNamePrefix) {
    Type t;

    if (desc.startsWith("(")) {
      t = Type.getReturnType(desc);
    } else {
      t = Type.getType(desc);
    }
    switch (t.getSort()) {
      case Type.DOUBLE:
        mv.visitInsn(DUP2);
        mv.visitMethodInsn(
            INVOKESTATIC, Config.instance.analysisClass, methodNamePrefix + "double", "(D)V", false);
        break;
      case Type.LONG:
        mv.visitInsn(DUP2);
        mv.visitMethodInsn(
            INVOKESTATIC, Config.instance.analysisClass, methodNamePrefix + "long", "(J)V", false);
        break;
      case Type.ARRAY:
        mv.visitInsn(DUP);
        mv.visitMethodInsn(
            INVOKESTATIC,
            Config.instance.analysisClass,
            methodNamePrefix + "Object",
            "(Ljava/lang/Object;)V", false);
        break;
      case Type.BOOLEAN:
        mv.visitInsn(DUP);
        mv.visitMethodInsn(
            INVOKESTATIC, Config.instance.analysisClass, methodNamePrefix + "boolean", "(Z)V", false);
        break;
      case Type.BYTE:
        mv.visitInsn(DUP);
        mv.visitMethodInsn(
            INVOKESTATIC, Config.instance.analysisClass, methodNamePrefix + "byte", "(B)V", false);
        break;
      case Type.CHAR:
        mv.visitInsn(DUP);
        mv.visitMethodInsn(
            INVOKESTATIC, Config.instance.analysisClass, methodNamePrefix + "char", "(C)V", false);
        break;
      case Type.FLOAT:
        mv.visitInsn(DUP);
        mv.visitMethodInsn(
            INVOKESTATIC, Config.instance.analysisClass, methodNamePrefix + "float", "(F)V", false);
        break;
      case Type.INT:
        mv.visitInsn(DUP);
        mv.visitMethodInsn(
            INVOKESTATIC, Config.instance.analysisClass, methodNamePrefix + "int", "(I)V", false);
        break;
      case Type.OBJECT:
        mv.visitInsn(DUP);
        mv.visitMethodInsn(
            INVOKESTATIC,
            Config.instance.analysisClass,
            methodNamePrefix + "Object",
            "(Ljava/lang/Object;)V", false);
        break;
      case Type.SHORT:
        mv.visitInsn(DUP);
        mv.visitMethodInsn(
            INVOKESTATIC, Config.instance.analysisClass, methodNamePrefix + "short", "(S)V", false);
        break;
      case Type.VOID:
        mv.visitMethodInsn(
            INVOKESTATIC, Config.instance.analysisClass, methodNamePrefix + "void", "()V", false);
        break;
      default:
        System.err.println("Unknown field or method descriptor " + desc);
        System.exit(1);
    }
  }
}
