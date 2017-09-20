
package janala.instrument;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


public class SnoopInstructionClassAdapter extends ClassVisitor {
  private final String cname;

  public SnoopInstructionClassAdapter(ClassVisitor cv, String cname) {
    super(Opcodes.ASM5, cv);
    this.cname = cname;
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, 
      String signature, String[] exceptions) {
    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    if (mv != null) {
      return new SnoopInstructionMethodAdapter(mv, cname, name, desc, 
          GlobalStateForInstrumentation.instance);
    }
    return null;
  }
}
