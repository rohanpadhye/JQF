package janala.logger.inst;

public class IF_ICMPNE extends Instruction implements ConditionalBranch {
  int label;

  public IF_ICMPNE(int iid, int mid, int label) {
    super(iid, mid);
    this.label = label;
  }

  public void visit(IVisitor visitor) {
    visitor.visitIF_ICMPNE(this);
  }

  @Override
  public String toString() {
    return "IF_ICMPNE iid=" + iid + " mid=" + mid + " label=" + label;
  }
}
