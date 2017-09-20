package janala.logger.inst;

public class MONITOREXIT extends Instruction {
  public MONITOREXIT(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitMONITOREXIT(this);
  }

  @Override
  public String toString() {
    return "MONITOREXIT iid=" + iid + " mid=" + mid;
  }
}
