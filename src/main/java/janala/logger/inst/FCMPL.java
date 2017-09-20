package janala.logger.inst;

public class FCMPL extends Instruction {
  public FCMPL(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitFCMPL(this);
  }

  @Override
  public String toString() {
    return "FCMPL iid=" + iid + " mid=" + mid;
  }
}
