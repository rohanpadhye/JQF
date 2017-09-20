package janala.logger.inst;

public class INEG extends Instruction {
  public INEG(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitINEG(this);
  }

  @Override
  public String toString() {
    return "INEG iid=" + iid + " mid=" + mid;
  }
}
