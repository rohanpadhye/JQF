package janala.logger.inst;

public class IADD extends Instruction {
  public IADD(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitIADD(this);
  }

  @Override
  public String toString() {
    return "IADD iid=" + iid + " mid=" + mid;
  }
}
