package janala.logger.inst;

public class F2L extends Instruction {
  public F2L(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitF2L(this);
  }

  @Override
  public String toString() {
    return "F2L iid=" + iid + " mid=" + mid;
  }
}
