package janala.logger.inst;

public class IMUL extends Instruction {
  public IMUL(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitIMUL(this);
  }

  @Override
  public String toString() {
    return "IMUL iid=" + iid + " mid=" + mid;
  }
}
