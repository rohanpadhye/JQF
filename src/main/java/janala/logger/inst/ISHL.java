package janala.logger.inst;

public class ISHL extends Instruction {
  public ISHL(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitISHL(this);
  }

  @Override
  public String toString() {
    return "ISHL iid=" + iid + " mid=" + mid;
  }
}
