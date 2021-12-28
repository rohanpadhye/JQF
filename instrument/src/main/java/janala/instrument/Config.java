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
  public final boolean useFastCoverageInstrumentation;

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

      useFastCoverageInstrumentation = Boolean.parseBoolean(properties.getProperty("useFastNonCollidingCoverageInstrumentation", "false"));
      if(useFastCoverageInstrumentation){
          analysisClass = "edu/berkeley/cs/jqf/instrument/tracing/FastCoverageSnoop";
      } else {
          analysisClass =
                  properties.getProperty("janala.snoopClass", "edu.berkeley.cs.jqf.instrument.tracing.SingleSnoop")
                          .replace('.', '/');
      }


      instrumentHeapLoad = Boolean.parseBoolean(properties.getProperty("janala.instrumentHeapLoad", "false"));
      instrumentAlloc = Boolean.parseBoolean(properties.getProperty("janala.instrumentAlloc", "false"));

      if((instrumentAlloc || instrumentHeapLoad) && useFastCoverageInstrumentation){
          throw new UnsupportedOperationException("It is currently not possible to use allocation or heap load tracking in conjunction with fast coverage");
      }


      String excludeInstStr = properties.getProperty("janala.excludes", null);
      if (excludeInstStr != null) {
          excludeInst = excludeInstStr.replace('.', '/').split(",");
      } else {
          excludeInst = new String[0];
      }

      String includeInstStr = properties.getProperty("janala.includes", null);
      if (includeInstStr != null) {
          includeInst = includeInstStr.replace('.', '/').split(",");
      } else {
          includeInst = new String[0];
      }

      instrumentationCacheDir = properties.getProperty("janala.instrumentationCacheDir");

  }
}
