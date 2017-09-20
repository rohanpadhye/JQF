package janala.logger.inst;

public class L2D extends Instruction {
  public L2D(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitL2D(this);
  }

  @Override
  public String toString() {
    return "L2D iid=" + iid + " mid=" + mid;
  }
}
