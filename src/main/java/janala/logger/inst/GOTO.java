package janala.logger.inst;

public class GOTO extends Instruction {
  int label;

  public GOTO(int iid, int mid, int label) {
    super(iid, mid);
    this.label = label;
  }

  public void visit(IVisitor visitor) {
    visitor.visitGOTO(this);
  }

  @Override
  public String toString() {
    return "GOTO iid=" + iid + " mid=" + mid + " label=" + label;
  }
}
