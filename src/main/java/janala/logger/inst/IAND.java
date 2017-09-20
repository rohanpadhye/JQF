package janala.logger.inst;

public class IAND extends Instruction {
  public IAND(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitIAND(this);
  }

  @Override
  public String toString() {
    return "IAND iid=" + iid + " mid=" + mid;
  }
}
