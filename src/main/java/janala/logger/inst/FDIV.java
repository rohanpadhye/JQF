package janala.logger.inst;

public class FDIV extends Instruction {
  public FDIV(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitFDIV(this);
  }

  @Override
  public String toString() {
    return "FDIV iid=" + iid + " mid=" + mid;
  }
}
