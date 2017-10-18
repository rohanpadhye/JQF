package janala.logger.inst;

public class IF_ICMPLT extends Instruction implements ConditionalBranch {
  int label;

  public IF_ICMPLT(int iid, int mid, int label) {
    super(iid, mid);
    this.label = label;
  }

  public void visit(IVisitor visitor) {
    visitor.visitIF_ICMPLT(this);
  }

  @Override
  public String toString() {
    return "IF_ICMPLT iid=" + iid + " mid=" + mid + " label=" + label;
  }
}
