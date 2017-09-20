package janala.logger.inst;

public class DASTORE extends Instruction {
  public DASTORE(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitDASTORE(this);
  }

  @Override
  public String toString() {
    return "DASTORE iid=" + iid + " mid=" + mid;
  }
}
