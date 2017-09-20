package janala.logger.inst;

public class ATHROW extends Instruction {
  public ATHROW(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitATHROW(this);
  }

  @Override
  public String toString() {
    return "ATHROW iid=" + iid + " mid=" + mid;
  }
}
