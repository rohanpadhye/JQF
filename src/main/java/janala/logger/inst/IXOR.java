package janala.logger.inst;

public class IXOR extends Instruction {
  public IXOR(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitIXOR(this);
  }

  @Override
  public String toString() {
    return "IXOR iid=" + iid + " mid=" + mid;
  }
}
