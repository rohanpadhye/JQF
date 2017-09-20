package janala.logger.inst;

public class LMUL extends Instruction {
  public LMUL(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitLMUL(this);
  }

  @Override
  public String toString() {
    return "LMUL iid=" + iid + " mid=" + mid;
  }
}
