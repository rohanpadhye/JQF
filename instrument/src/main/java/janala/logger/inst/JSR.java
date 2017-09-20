package janala.logger.inst;

public class JSR extends Instruction {
  int label;

  public JSR(int iid, int mid, int label) {
    super(iid, mid);
    this.label = label;
  }

  public void visit(IVisitor visitor) {
    visitor.visitJSR(this);
  }

  @Override
  public String toString() {
    return "JSR iid=" + iid + " mid=" + mid + " label=" + label;
  }
}
