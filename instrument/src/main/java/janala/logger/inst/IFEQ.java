package janala.logger.inst;

public class IFEQ extends Instruction implements ConditionalBranch {
  int label;

  public IFEQ(int iid, int mid, int label) {
    super(iid, mid);
    this.label = label;
  }

  public void visit(IVisitor visitor) {
    visitor.visitIFEQ(this);
  }

  @Override
  public String toString() {
    return "IFEQ iid=" + iid + " mid=" + mid + " label=" + label;
  }
}
