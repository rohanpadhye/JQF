package janala.logger.inst;

public class DCONST_0 extends Instruction {
  public DCONST_0(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitDCONST_0(this);
  }

  @Override
  public String toString() {
    return "DCONST_0 iid=" + iid + " mid=" + mid;
  }
}
