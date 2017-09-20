package janala.logger.inst;

public class FSUB extends Instruction {
  public FSUB(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitFSUB(this);
  }

  @Override
  public String toString() {
    return "FSUB iid=" + iid + " mid=" + mid;
  }
}
