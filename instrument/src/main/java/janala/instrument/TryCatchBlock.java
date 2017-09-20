package janala.instrument;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

/**
 * Author: Koushik Sen (ksen@cs.berkeley.edu)
 * Date: 7/1/12
 * Time: 10:50 PM
 */
public class TryCatchBlock {
  Label label;
  Label label1;
  Label label2;
  String type;

  public TryCatchBlock(Label label, Label label1, Label label2, String type) {
    this.label = label;
    this.label1 = label1;
    this.label2 = label2;
    this.type = type;
  }

  public void visit(MethodVisitor mv) {
    mv.visitTryCatchBlock(label, label1, label2, type);
  }
}
