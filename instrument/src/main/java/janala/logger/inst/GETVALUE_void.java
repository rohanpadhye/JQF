package janala.logger.inst;

public class GETVALUE_void extends Instruction implements GETVALUE {

  public GETVALUE_void() {
    super(-1, -1);
  }

  public void visit(IVisitor visitor) {
    visitor.visitGETVALUE_void(this);
  }

  @Override
  public String toString() {
    return "GETVALUE_void";
  }
}
