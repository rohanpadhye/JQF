package janala.logger.inst;

public class DALOAD extends Instruction {
  public DALOAD(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitDALOAD(this);
  }

  @Override
  public String toString() {
    return "DALOAD iid=" + iid + " mid=" + mid;
  }
}
