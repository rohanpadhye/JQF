package janala.logger.inst;

public class LNEG extends Instruction {
  public LNEG(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitLNEG(this);
  }

  @Override
  public String toString() {
    return "LNEG iid=" + iid + " mid=" + mid;
  }
}
