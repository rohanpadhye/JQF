package janala.logger.inst;

public class D2L extends Instruction {
  public D2L(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitD2L(this);
  }

  @Override
  public String toString() {
    return "D2L iid=" + iid + " mid=" + mid;
  }
}
