package janala.logger.inst;

public class DSUB extends Instruction {
  public DSUB(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitDSUB(this);
  }

  @Override
  public String toString() {
    return "DSUB iid=" + iid + " mid=" + mid;
  }
}
