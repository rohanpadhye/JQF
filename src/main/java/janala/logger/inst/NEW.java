package janala.logger.inst;

public class NEW extends Instruction {
  String type;
  public int cIdx;

  public NEW(int iid, int mid, String type, int cIdx) {
    super(iid, mid);
    this.type = type;
    this.cIdx = cIdx;
  }

  public void visit(IVisitor visitor) {
    visitor.visitNEW(this);
  }

  @Override
  public String toString() {
    return "NEW iid=" + iid + " mid=" + mid + " cIdx=" + cIdx;
  }
}
