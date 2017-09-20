package janala.logger.inst;

public class ICONST_M1 extends Instruction {
  public ICONST_M1(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitICONST_M1(this);
  }

  @Override
  public String toString() {
    return "ICONST_M1 iid=" + iid + " mid=" + mid;
  }
}
