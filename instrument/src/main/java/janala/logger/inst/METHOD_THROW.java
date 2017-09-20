package janala.logger.inst;

/** A special marker instruction indicating that the method
 * is exiting abruptly due to an exception being thrown, and thus
 * the stack frame must be destroyed.
  */
public class METHOD_THROW extends Instruction {

  public METHOD_THROW() {
    super(-1, -1);
  }

  public void visit(IVisitor visitor) {
    visitor.visitMETHOD_THROW(this);
  }

  @Override
  public String toString() {
    return "METHOD_THROW";
  }
}
