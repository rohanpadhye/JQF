package janala.logger.inst;

public class FNEG extends Instruction {
  public FNEG(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitFNEG(this);
  }

  @Override
  public String toString() {
    return "FNEG iid=" + iid + " mid=" + mid;
  }
}
