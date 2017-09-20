package janala.logger.inst;

public class DRETURN extends Instruction {
  public DRETURN(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitDRETURN(this);
  }

  @Override
  public String toString() {
    return "DRETURN iid=" + iid + " mid=" + mid;
  }
}
