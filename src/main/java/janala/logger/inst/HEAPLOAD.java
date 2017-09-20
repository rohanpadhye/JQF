package janala.logger.inst;

public class HEAPLOAD extends Instruction {
  public int objectId;
  public String field;

  public HEAPLOAD(int iid, int mid, int objectId, String field) {
    super(iid, mid);
    this.objectId = objectId;
    this.field = field;
  }

  public void visit(IVisitor visitor) {
    visitor.visitHEAPLOAD(this);
  }

  @Override
  public String toString() {
    return "HEAPLOAD iid="
        + iid
        + " mid="
        + mid
        + " objectId="
        + objectId
        + " field="
        + field;
  }
}
