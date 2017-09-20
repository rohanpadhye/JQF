package janala.logger.inst;

public class NEWARRAY extends Instruction {
  public NEWARRAY(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitNEWARRAY(this);
  }

  @Override
  public String toString() {
    return "NEWARRAY iid=" + iid + " mid=" + mid;
  }
}
