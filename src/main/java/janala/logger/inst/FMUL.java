package janala.logger.inst;

public class FMUL extends Instruction {
  public FMUL(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitFMUL(this);
  }

  @Override
  public String toString() {
    return "FMUL iid=" + iid + " mid=" + mid;
  }
}
