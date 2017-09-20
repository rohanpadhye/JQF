package janala.logger.inst;

public class FSTORE extends Instruction {
  public int var;

  public FSTORE(int iid, int mid, int var) {
    super(iid, mid);
    this.var = var;
  }

  public void visit(IVisitor visitor) {
    visitor.visitFSTORE(this);
  }

  @Override
  public String toString() {
    return "FSTORE iid=" + iid + " mid=" + mid + " var=" + var;
  }
}
