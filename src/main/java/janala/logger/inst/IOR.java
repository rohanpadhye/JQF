package janala.logger.inst;

public class IOR extends Instruction {
  public IOR(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitIOR(this);
  }

  @Override
  public String toString() {
    return "IOR iid=" + iid + " mid=" + mid;
  }
}
