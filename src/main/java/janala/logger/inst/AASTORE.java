package janala.logger.inst;

public class AASTORE extends Instruction {
  public AASTORE(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitAASTORE(this);
  }

  @Override
  public String toString() {
    return "AASTORE iid=" + iid + " mid=" + mid;
  }
}
