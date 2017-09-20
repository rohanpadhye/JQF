package janala.logger.inst;

public class DCMPL extends Instruction {
  public DCMPL(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitDCMPL(this);
  }

  @Override
  public String toString() {
    return "DCMPL iid=" + iid + " mid=" + mid;
  }
}
