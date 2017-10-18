package janala.logger.inst;

public class IFNULL extends Instruction implements ConditionalBranch {
  int label;

  public IFNULL(int iid, int mid, int label) {
    super(iid, mid);
    this.label = label;
  }

  public void visit(IVisitor visitor) {
    visitor.visitIFNULL(this);
  }

  @Override
  public String toString() {
    return "IFNULL iid=" + iid + " mid=" + mid + " label=" + label;
  }
}
