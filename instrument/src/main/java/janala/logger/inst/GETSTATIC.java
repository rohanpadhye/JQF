package janala.logger.inst;

public class GETSTATIC extends Instruction {
  public int cIdx;
  public int fIdx;
  public String desc;

  public GETSTATIC(int iid, int mid, int cIdx, int fIdx, String desc) {
    super(iid, mid);
    this.cIdx = cIdx;
    this.fIdx = fIdx;
    this.desc = desc;
  }

  public void visit(IVisitor visitor) {
    visitor.visitGETSTATIC(this);
  }

  @Override
  public String toString() {
    return "GETSTATIC iid="
        + iid
        + " mid="
        + mid
        + " cIdx="
        + cIdx
        + " fIdx="
        + fIdx
        + " desc="
        + desc;
  }
}
