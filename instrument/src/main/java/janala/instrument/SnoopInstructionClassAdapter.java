
package janala.instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


public class SnoopInstructionClassAdapter extends ClassVisitor {
  private final String className;
  private String superName;

  public SnoopInstructionClassAdapter(ClassVisitor cv, String className) {
    super(Opcodes.ASM5, cv);
    this.className = className;
  }

  @Override
  public void visit(int version,
                    int access,
                    String name,
                    String signature,
                    String superName,
                    String[] interfaces) {
    assert (name == this.className);
    this.superName = superName;
    cv.visit(version, access, name, signature, superName, interfaces);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, 
      String signature, String[] exceptions) {
    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    if (mv != null) {
      return new SnoopInstructionMethodAdapter(mv, className, name, desc, superName,
          GlobalStateForInstrumentation.instance);
    }
    return null;
  }
}
