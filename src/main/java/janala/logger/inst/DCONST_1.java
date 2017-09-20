package janala.logger.inst;

public class DCONST_1 extends Instruction {
  public DCONST_1(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitDCONST_1(this);
  }

  @Override
  public String toString() {
    return "DCONST_1 iid=" + iid + " mid=" + mid;
  }
}
