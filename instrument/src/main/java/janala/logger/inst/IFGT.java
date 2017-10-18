package janala.logger.inst;

public class IFGT extends Instruction implements ConditionalBranch {
  int label;

  public IFGT(int iid, int mid, int label) {
    super(iid, mid);
    this.label = label;
  }

  public void visit(IVisitor visitor) {
    visitor.visitIFGT(this);
  }

  @Override
  public String toString() {
    return "IFGT iid=" + iid + " mid=" + mid + " label=" + label;
  }
}
