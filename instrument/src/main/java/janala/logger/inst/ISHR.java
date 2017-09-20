package janala.logger.inst;

public class ISHR extends Instruction {
  public ISHR(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitISHR(this);
  }

  @Override
  public String toString() {
    return "ISHR iid=" + iid + " mid=" + mid;
  }
}
