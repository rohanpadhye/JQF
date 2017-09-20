package janala.logger.inst;

public class ISTORE extends Instruction {
  public int var;

  public ISTORE(int iid, int mid, int var) {
    super(iid, mid);
    this.var = var;
  }

  public void visit(IVisitor visitor) {
    visitor.visitISTORE(this);
  }

  @Override
  public String toString() {
    return "ISTORE iid=" + iid + " mid=" + mid + " var=" + var;
  }
}
