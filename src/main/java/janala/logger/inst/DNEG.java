package janala.logger.inst;

public class DNEG extends Instruction {
  public DNEG(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitDNEG(this);
  }

  @Override
  public String toString() {
    return "DNEG iid=" + iid + " mid=" + mid;
  }
}
