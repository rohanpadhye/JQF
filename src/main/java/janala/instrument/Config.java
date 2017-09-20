package janala.instrument;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class Config {
  // System properties
  public static final String propFile = System.getProperty("janala.conf", "janala.conf");

  public static final Config instance = new Config();

  public boolean verbose = true;
  public boolean writeInstrumentedClasses = true;
  public String analysisClass = "janala/logger/StringLogger";
  public String[] excludeInst = new String[0];
  public String[] includeInst  = new String[0];
  public boolean instrumentHeapLoad = false;

  public Config() {
      Properties properties = new Properties();
      try(InputStream propStream = new FileInputStream(propFile)) {
        properties.load(propStream);
      } catch (IOException e) {
        // Swallow exception and continue with defaults
        e.printStackTrace();
        return;
      }

      verbose = Boolean.getBoolean(properties.getProperty("janala.isVerbose", "false"));
      writeInstrumentedClasses = Boolean.getBoolean(properties.getProperty("janala.writeInstrumentedClasses", "true"));
      analysisClass =
              properties.getProperty("janala.snoopClass", "janala.instrument.SnoopLogger").replace('.', '/');
      String excludeInstStr = properties.getProperty("janala.excludes", "");
      if (excludeInstStr.length() > 0)
        excludeInst = excludeInstStr.split(",");
      else
        excludeInst = new String[0];
      String includeInstStr = properties.getProperty("janala.includes", "");
      if (includeInstStr.length() > 0)
        includeInst = includeInstStr.split(",");
      instrumentHeapLoad = Boolean.getBoolean(properties.getProperty("janala.instrumentHeapLoad", "false"));

  }
}
