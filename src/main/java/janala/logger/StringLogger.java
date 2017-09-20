package janala.logger;

import janala.logger.inst.Instruction;

public class StringLogger extends AbstractLogger {

  private StringBuffer buffer;
  private boolean init;

  public StringLogger() {
    buffer = new StringBuffer();
    init = true;
  }

  @Override
  protected void log(Instruction insn) {
    if (!init) {
      buffer.append("\n");
    } else {
      init = false;
    }
    buffer.append(insn.toString());
  }

  public String getLog() {
    return buffer.toString();
  }
}
