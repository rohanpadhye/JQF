package janala.logger.inst;

public class LLOAD extends Instruction {
  public int var;

  public LLOAD(int iid, int mid, int var) {
    super(iid, mid);
    this.var = var;
  }

  public void visit(IVisitor visitor) {
    visitor.visitLLOAD(this);
  }

  @Override
  public String toString() {
    return "LLOAD iid=" + iid + " mid=" + mid + " var=" + var;
  }
}
