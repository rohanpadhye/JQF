package janala.logger.inst;

public class INVOKEMETHOD_EXCEPTION extends Instruction {

  private Throwable exception;

  public INVOKEMETHOD_EXCEPTION(Throwable t) {
    super(-1, -1);
    this.exception = t;
  }

  public void visit(IVisitor visitor) {
    visitor.visitINVOKEMETHOD_EXCEPTION(this);
  }

  @Override
  public String toString() {
    return "INVOKEMETHOD_EXCEPTION";
  }

  public Throwable getException() {
    return exception;
  }
}
