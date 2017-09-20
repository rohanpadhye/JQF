package janala.logger.inst;

public class CHECKCAST extends Instruction {
  String type;

  public CHECKCAST(int iid, int mid, String type) {
    super(iid, mid);
    this.type = type;
  }

  public void visit(IVisitor visitor) {
    visitor.visitCHECKCAST(this);
  }

  @Override
  public String toString() {
    return "CHECKCAST iid=" + iid + " mid=" + mid + " type=" + type;
  }
}
