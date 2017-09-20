package janala.logger.inst;

public class INVOKEMETHOD_EXCEPTION extends Instruction {

  public INVOKEMETHOD_EXCEPTION() {
    super(-1, -1);
  }

  public void visit(IVisitor visitor) {
    visitor.visitINVOKEMETHOD_EXCEPTION(this);
  }

  @Override
  public String toString() {
    return "INVOKEMETHOD_EXCEPTION";
  }
}
