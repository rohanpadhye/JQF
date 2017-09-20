package janala.logger.inst;

public class L2F extends Instruction {
  public L2F(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitL2F(this);
  }

  @Override
  public String toString() {
    return "L2F iid=" + iid + " mid=" + mid;
  }
}
