package janala.logger.inst;

public class GETVALUE_char extends Instruction implements GETVALUE {
  public char v;

  public GETVALUE_char(char v) {
    super(-1, -1);
    this.v = v;
  }

  public void visit(IVisitor visitor) {
    visitor.visitGETVALUE_char(this);
  }

  @Override
  public String toString() {
    return "GETVALUE_char v=" + v;
  }
}
