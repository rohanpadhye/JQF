package janala.logger.inst;

public class POP2 extends Instruction {
  public POP2(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitPOP2(this);
  }

  @Override
  public String toString() {
    return "POP2 iid=" + iid + " mid=" + mid;
  }
}
