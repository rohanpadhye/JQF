package janala.logger.inst;

public class GETVALUE_short extends Instruction implements GETVALUE {
  public short v;

  public GETVALUE_short(short v) {
    super(-1, -1);
    this.v = v;
  }

  public void visit(IVisitor visitor) {
    visitor.visitGETVALUE_short(this);
  }

  @Override
  public String toString() {
    return "GETVALUE_short v=" + v;
  }
}
