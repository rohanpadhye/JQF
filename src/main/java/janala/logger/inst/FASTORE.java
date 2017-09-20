package janala.logger.inst;

public class FASTORE extends Instruction {
  public FASTORE(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitFASTORE(this);
  }

  @Override
  public String toString() {
    return "FASTORE iid=" + iid + " mid=" + mid;
  }
}
