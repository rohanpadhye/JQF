package janala.logger.inst;

public class ICONST_2 extends Instruction {
  public ICONST_2(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitICONST_2(this);
  }

  @Override
  public String toString() {
    return "ICONST_2 iid=" + iid + " mid=" + mid;
  }
}
