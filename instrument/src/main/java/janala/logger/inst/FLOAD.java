package janala.logger.inst;

public class FLOAD extends Instruction {
  public int var;

  public FLOAD(int iid, int mid, int var) {
    super(iid, mid);
    this.var = var;
  }

  public void visit(IVisitor visitor) {
    visitor.visitFLOAD(this);
  }

  @Override
  public String toString() {
    return "FLOAD iid=" + iid + " mid=" + mid + " var=" + var;
  }
}
