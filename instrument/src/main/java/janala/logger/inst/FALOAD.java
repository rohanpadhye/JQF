package janala.logger.inst;

public class FALOAD extends Instruction {
  public FALOAD(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitFALOAD(this);
  }

  @Override
  public String toString() {
    return "FALOAD iid=" + iid + " mid=" + mid;
  }
}
