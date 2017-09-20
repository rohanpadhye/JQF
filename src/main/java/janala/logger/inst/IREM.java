package janala.logger.inst;

public class IREM extends Instruction {
  public IREM(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitIREM(this);
  }

  @Override
  public String toString() {
    return "IREM iid=" + iid + " mid=" + mid;
  }
}
