package janala.logger.inst;

public class FREM extends Instruction {
  public FREM(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitFREM(this);
  }

  @Override
  public String toString() {
    return "FREM iid=" + iid + " mid=" + mid;
  }
}
