package janala.logger.inst;

public class ARRAYLENGTH extends Instruction {
  public ARRAYLENGTH(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitARRAYLENGTH(this);
  }

  @Override
  public String toString() {
    return "ARRAYLENGTH iid=" + iid + " mid=" + mid;
  }
}
