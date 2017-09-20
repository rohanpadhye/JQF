package janala.logger.inst;


public class LDC_Object extends Instruction {
  public int c;

  public LDC_Object(int iid, int mid, int c) {
    super(iid, mid);
    this.c = c;
  }

  public void visit(IVisitor visitor) {
    visitor.visitLDC_Object(this);
  }

  @Override
  public String toString() {
    return "LDC_Object iid=" + iid + " mid=" + mid + " c=" + c;
  }
}
