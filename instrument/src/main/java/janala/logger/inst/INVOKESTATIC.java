package janala.logger.inst;

public class INVOKESTATIC extends Instruction implements InvokeInstruction {
  public String owner;
  public String name;
  public String desc;

  public INVOKESTATIC(int iid, int mid, String owner, String name, String desc) {
    super(iid, mid);
    this.owner = owner;
    this.name = name;
    this.desc = desc;
  }

  public void visit(IVisitor visitor) {
    visitor.visitINVOKESTATIC(this);
  }

  @Override
  public String toString() {
    return "INVOKESTATIC iid="
        + iid
        + " mid="
        + mid
        + " owner="
        + owner
        + " name="
        + name
        + " desc="
        + desc;
  }


  @Override
  public String getOwner() {
    return owner;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDesc() {
    return desc;
  }
}
