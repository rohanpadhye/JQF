package janala.logger.inst;

public class DCMPG extends Instruction {
  public DCMPG(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitDCMPG(this);
  }

  @Override
  public String toString() {
    return "DCMPG iid=" + iid + " mid=" + mid;
  }
}
