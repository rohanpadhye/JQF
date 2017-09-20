package janala.logger.inst;

public class ICONST_5 extends Instruction {
  public ICONST_5(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitICONST_5(this);
  }

  @Override
  public String toString() {
    return "ICONST_5 iid=" + iid + " mid=" + mid;
  }
}
