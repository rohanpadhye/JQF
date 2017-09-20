package janala.logger.inst;

public class IDIV extends Instruction {
  public IDIV(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitIDIV(this);
  }

  @Override
  public String toString() {
    return "IDIV iid=" + iid + " mid=" + mid;
  }
}
