package janala.logger.inst;

public class F2D extends Instruction {
  public F2D(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitF2D(this);
  }

  @Override
  public String toString() {
    return "F2D iid=" + iid + " mid=" + mid;
  }
}
