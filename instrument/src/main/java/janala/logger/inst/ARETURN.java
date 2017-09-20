package janala.logger.inst;

public class ARETURN extends Instruction {
  public ARETURN(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitARETURN(this);
  }

  @Override
  public String toString() {
    return "ARETURN iid=" + iid + " mid=" + mid;
  }
}
