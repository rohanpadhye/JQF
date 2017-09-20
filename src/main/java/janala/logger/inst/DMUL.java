package janala.logger.inst;

public class DMUL extends Instruction {
  public DMUL(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitDMUL(this);
  }

  @Override
  public String toString() {
    return "DMUL iid=" + iid + " mid=" + mid;
  }
}
