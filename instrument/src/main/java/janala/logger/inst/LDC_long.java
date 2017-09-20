package janala.logger.inst;

public class LDC_long extends Instruction {
  public long c;

  public LDC_long(int iid, int mid, long c) {
    super(iid, mid);
    this.c = c;
  }

  public void visit(IVisitor visitor) {
    visitor.visitLDC_long(this);
  }

  @Override
  public String toString() {
    return "LDC_long iid=" + iid + " mid=" + mid + " c=" + c;
  }
}
