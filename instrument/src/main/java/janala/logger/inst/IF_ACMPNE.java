package janala.logger.inst;

public class IF_ACMPNE extends Instruction implements ConditionalBranch {
  int label;

  public IF_ACMPNE(int iid, int mid, int label) {
    super(iid, mid);
    this.label = label;
  }

  public void visit(IVisitor visitor) {
    visitor.visitIF_ACMPNE(this);
  }

  @Override
  public String toString() {
    return "IF_ACMPNE iid=" + iid + " mid=" + mid + " label=" + label;
  }
}
