package janala.logger.inst;

public class GETVALUE_Object extends Instruction implements GETVALUE {
  public int v;
  public boolean isString;
  public String string;

  public GETVALUE_Object(int v, String string, boolean isString) {
    super(-1, -1);
    this.v = v;
    this.string = string;
    this.isString = isString;
  }

  public void visit(IVisitor visitor) {
    visitor.visitGETVALUE_Object(this);
  }

  @Override
  public String toString() {
    return "GETVALUE_Object v="
      + Integer.toHexString(v)
      + " isString="
      + isString
      + (isString ? " string=" + string : "");
  }
}
