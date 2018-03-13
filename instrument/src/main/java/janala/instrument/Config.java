package janala.instrument;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

class Config {
  // System properties
  public static final String propFile = System.getProperty("janala.conf", "janala.conf");

  public static final Config instance = new Config();

  public final boolean verbose;
  public final String analysisClass;
  public final String[] excludeInst;
  public final String[] includeInst;
  public final boolean instrumentHeapLoad;
  public final boolean instrumentAlloc;
  public final String instrumentationCacheDir;

  private Config() {
      // Read properties from the conf file
      Properties properties = new Properties();
      try (InputStream propStream = new FileInputStream(propFile)) {
          properties.load(propStream);
      } catch (IOException e) {
          // Swallow exception and continue with defaults
          // System.err.println("Warning: No janala.conf file found");
      }

      // Let JVM command-line properties override these
      properties.putAll(System.getProperties());

      verbose = Boolean.parseBoolean(properties.getProperty("janala.verbose", "false"));

      analysisClass =
              properties.getProperty("janala.snoopClass", "edu.berkeley.cs.jqf.instrument.tracing.SingleSnoop")
                      .replace('.', '/');


      instrumentHeapLoad = Boolean.parseBoolean(properties.getProperty("janala.instrumentHeapLoad", "false"));
      instrumentAlloc = Boolean.parseBoolean(properties.getProperty("janala.instrumentAlloc", "false"));

      String excludeInstStr = properties.getProperty("janala.excludes", "");
      if (excludeInstStr.length() > 0) {
          excludeInst = excludeInstStr.replace('.', '/').split(",");
      } else {
          excludeInst = new String[0];
      }

      String includeInstStr = properties.getProperty("janala.includes", "");
      if (includeInstStr.length() > 0) {
          includeInst = includeInstStr.replace('.', '/').split(",");
      } else {
          includeInst = new String[0];
      }

      instrumentationCacheDir = properties.getProperty("janala.instrumentationCacheDir");

  }
}
