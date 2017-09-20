package janala.logger.inst;

public class INVOKEMETHOD_END extends Instruction {

  public INVOKEMETHOD_END() {
    super(-1, -1);
  }

  public void visit(IVisitor visitor) {
    visitor.visitINVOKEMETHOD_END(this);
  }

  @Override
  public String toString() {
    return "INVOKEMETHOD_END";
  }
}
