package janala.logger.inst;

public class DUP_X2 extends Instruction {
  public DUP_X2(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitDUP_X2(this);
  }

  @Override
  public String toString() {
    return "DUP_X2 iid=" + iid + " mid=" + mid;
  }
}
