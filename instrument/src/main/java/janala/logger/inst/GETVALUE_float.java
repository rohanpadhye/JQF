package janala.logger.inst;

public class GETVALUE_float extends Instruction implements GETVALUE {
  public float v;

  public GETVALUE_float(float v) {
    super(-1, -1);
    this.v = v;
  }

  public void visit(IVisitor visitor) {
    visitor.visitGETVALUE_float(this);
  }

  @Override
  public String toString() {
    return "GETVALUE_float v=" + v;
  }
}
