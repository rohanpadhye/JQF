package janala.logger.inst;

public class SASTORE extends Instruction {
  public SASTORE(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitSASTORE(this);
  }

  @Override
  public String toString() {
    return "SASTORE iid=" + iid + " mid=" + mid;
  }
}
