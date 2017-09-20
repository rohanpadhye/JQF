package janala.logger.inst;

public class ICONST_0 extends Instruction {
  public ICONST_0(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitICONST_0(this);
  }

  @Override
  public String toString() {
    return "ICONST_0 iid=" + iid + " mid=" + mid;
  }
}
