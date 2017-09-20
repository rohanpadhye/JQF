package janala.logger.inst;

/**
 * Author: Koushik Sen (ksen@cs.berkeley.edu)
 * Date: 6/21/12
 * Time: 5:02 PM
 */
public class MAKE_SYMBOLIC extends Instruction {

  public MAKE_SYMBOLIC() {
    super(-1, -1);
  }

  public void visit(IVisitor visitor) {
    visitor.visitMAKE_SYMBOLIC(this);
  }

  @Override
  public String toString() {
    return "MAKE_SYMBOLIC";
  }
}
