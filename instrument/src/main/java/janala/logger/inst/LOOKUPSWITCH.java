package janala.logger.inst;

public class LOOKUPSWITCH extends Instruction {
  public int dflt;
  public int[] keys;
  public int[] labels;

  public LOOKUPSWITCH(int iid, int mid, int dflt, int[] keys, int[] labels) {
    super(iid, mid);
    this.dflt = dflt;
    this.keys = keys;
    this.labels = labels;
  }

  public void visit(IVisitor visitor) {
    visitor.visitLOOKUPSWITCH(this);
  }

  @Override
  public String toString() {
    return "LOOKUPSWITCH iid="
        + iid
        + " mid="
        + mid
        + " dflt="
        + dflt
        + " keys="
        + keys
        + " labels="
        + labels;
  }
}
