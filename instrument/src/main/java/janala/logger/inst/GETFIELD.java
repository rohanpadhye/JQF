package janala.logger.inst;

public class GETFIELD extends Instruction {
  public int cIdx;
  public int fIdx;
  public String desc;

  public GETFIELD(int iid, int mid, int cIdx, int fIdx, String desc) {
    super(iid, mid);
    this.cIdx = cIdx;
    this.fIdx = fIdx;
    this.desc = desc;
  }

  public void visit(IVisitor visitor) {
    visitor.visitGETFIELD(this);
  }

  @Override
  public String toString() {
    return "GETFIELD iid="
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
