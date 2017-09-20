package janala.logger.inst;

public class LRETURN extends Instruction {
  public LRETURN(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitLRETURN(this);
  }

  @Override
  public String toString() {
    return "LRETURN iid=" + iid + " mid=" + mid;
  }
}
