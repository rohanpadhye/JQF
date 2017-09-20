package janala.logger.inst;

public class I2D extends Instruction {
  public I2D(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitI2D(this);
  }

  @Override
  public String toString() {
    return "I2D iid=" + iid + " mid=" + mid;
  }
}
