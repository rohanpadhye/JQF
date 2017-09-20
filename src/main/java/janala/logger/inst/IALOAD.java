package janala.logger.inst;

public class IALOAD extends Instruction {
  public IALOAD(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitIALOAD(this);
  }

  @Override
  public String toString() {
    return "IALOAD iid=" + iid + " mid=" + mid;
  }
}
