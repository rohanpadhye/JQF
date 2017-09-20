package janala.logger.inst;

public class RET extends Instruction {
  int var;

  public RET(int iid, int mid, int var) {
    super(iid, mid);
    this.var = var;
  }

  public void visit(IVisitor visitor) {
    visitor.visitRET(this);
  }

  @Override
  public String toString() {
    return "RET iid=" + iid + " mid=" + mid + " var=" + var;
  }
}
