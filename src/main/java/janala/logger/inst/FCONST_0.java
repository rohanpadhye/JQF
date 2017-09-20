package janala.logger.inst;

public class FCONST_0 extends Instruction {
  public FCONST_0(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitFCONST_0(this);
  }

  @Override
  public String toString() {
    return "FCONST_0 iid=" + iid + " mid=" + mid;
  }
}
