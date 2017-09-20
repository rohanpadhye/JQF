package janala.logger.inst;

public class ACONST_NULL extends Instruction {
  public ACONST_NULL(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitACONST_NULL(this);
  }

  @Override
  public String toString() {
    return "ACONST_NULL iid=" + iid + " mid=" + mid;
  }
}
