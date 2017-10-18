package janala.logger.inst;

public class IF_ICMPGE extends Instruction implements ConditionalBranch {
  int label;

  public IF_ICMPGE(int iid, int mid, int label) {
    super(iid, mid);
    this.label = label;
  }

  public void visit(IVisitor visitor) {
    visitor.visitIF_ICMPGE(this);
  }

  @Override
  public String toString() {
    return "IF_ICMPGE iid=" + iid + " mid=" + mid + " label=" + label;
  }
}
