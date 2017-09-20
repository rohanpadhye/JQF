package janala.logger.inst;

public class MONITORENTER extends Instruction {
  public MONITORENTER(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitMONITORENTER(this);
  }

  @Override
  public String toString() {
    return "MONITORENTER iid=" + iid + " mid=" + mid;
  }
}
