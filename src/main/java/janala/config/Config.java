package janala.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import janala.logger.Logger;

public class Config {
  // System properties
  public static final String propFile = System.getProperty("janala.conf", "janala.conf");

  public static final Config instance = new Config();

  public boolean verbose;
  public boolean writeInstrumentedClasses = true;
  public String analysisClass;
  public String[] excludeInst = new String[0];
  public String[] includeInst = new String[0];
  private String loggerClass;

  public Config() {
    try {
      Properties properties = new Properties();
      properties.load(new FileInputStream(propFile));

      verbose = properties.getProperty("janala.isVerbose", "false").equals("true");
      writeInstrumentedClasses = properties.getProperty("janala.writeInstrumentedClasses", "true").equals("true");
      loggerClass = System.getProperty("janala.loggerClass", "janala.logger.StringLogger");
      analysisClass =
          properties.getProperty("janala.snoopClass", "janala.instrument.SnoopLogger").replace('.', '/');
      String excludeInstStr = properties.getProperty("janala.excludes", "");
      if (excludeInstStr.length() > 0) 
        excludeInst = excludeInstStr.split(",");
      String includeInstStr = properties.getProperty("janala.includes", "");
      if (includeInstStr.length() > 0)
        includeInst = includeInstStr.split(",");
    } catch (IOException ex) {
      // Force exception from instrumented class
      analysisClass = "janala/conf/not/loaded/Error";
    }
  }

  private Object getObject(String className) {
    try {
      Class<?> clazz = Class.forName(className);
      Object ret = clazz.newInstance();
      return ret;
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      System.exit(1);
    } catch (InstantiationException e) {
      e.printStackTrace();
      System.exit(1);
    } catch (IllegalAccessException e) {
      e.printStackTrace();
      System.exit(1);
    }
    return null;
  }

  public Logger getLogger() {
    if (loggerClass == null || loggerClass.isEmpty()) {
      return null;
    }
    return (Logger) getObject(loggerClass);
  }
}
