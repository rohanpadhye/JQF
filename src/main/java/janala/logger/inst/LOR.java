package janala.logger.inst;

public class LOR extends Instruction {
  public LOR(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitLOR(this);
  }

  @Override
  public String toString() {
    return "LOR iid=" + iid + " mid=" + mid;
  }
}
