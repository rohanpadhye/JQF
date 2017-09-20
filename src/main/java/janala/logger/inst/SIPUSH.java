package janala.logger.inst;

public class SIPUSH extends Instruction {
  public int value;

  public SIPUSH(int iid, int mid, int value) {
    super(iid, mid);
    this.value = value;
  }

  public void visit(IVisitor visitor) {
    visitor.visitSIPUSH(this);
  }

  @Override
  public String toString() {
    return "SIPUSH iid=" + iid + " mid=" + mid + " value=" + value;
  }
}
