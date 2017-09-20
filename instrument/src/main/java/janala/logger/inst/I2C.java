package janala.logger.inst;

public class I2C extends Instruction {
  public I2C(int iid, int mid) {
    super(iid, mid);
  }

  public void visit(IVisitor visitor) {
    visitor.visitI2C(this);
  }

  @Override
  public String toString() {
    return "I2C iid=" + iid + " mid=" + mid;
  }
}
