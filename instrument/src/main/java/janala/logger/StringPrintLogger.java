package janala.logger;

import janala.logger.inst.Instruction;

public class StringPrintLogger extends AbstractLogger {
  private final StringLogger logger;

  private class FinishThread extends Thread {
    @Override
    public void run() {
      System.out.println("========== Instructions =========");
      System.out.println(logger.getLog());
      System.out.println("==========     End      =========");
    }
  }

  public StringPrintLogger() {
    logger = new StringLogger();
    Runtime.getRuntime().addShutdownHook(new FinishThread());
  }

  @Override
  protected void log(Instruction insn) {
    logger.log(insn);
  }


}
