package janala.logger.inst;

import java.io.Serializable;

public abstract class Instruction implements Serializable {
  public final int iid;
  public final int mid;

  public abstract void visit(IVisitor visitor);

  public Instruction(int iid, int mid) {
    this.iid = iid;
    this.mid = mid;
  }
}
