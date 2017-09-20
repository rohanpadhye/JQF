package janala.logger.inst;

public class DUP_X1 extends Instruction {
  public DUP_X1(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitDUP_X1(this);
  }

  @Override
  public String toString() {
    return "DUP_X1 iid=" + iid + " mid=" + mid;
  }
}
