package janala.logger.inst;

public class LREM extends Instruction {
  public LREM(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitLREM(this);
  }

  @Override
  public String toString() {
    return "LREM iid=" + iid + " mid=" + mid;
  }
}
