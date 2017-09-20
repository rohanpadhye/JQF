package janala.logger.inst;

public class DADD extends Instruction {
  public DADD(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitDADD(this);
  }

  @Override
  public String toString() {
    return "DADD iid=" + iid + " mid=" + mid;
  }
}
