package janala.instrument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

@SuppressWarnings("unused") // Registered via -javaagent
public class SnoopInstructionTransformer implements ClassFileTransformer {
  private static final String instDir = Config.instance.instrumentationCacheDir;
  private static final boolean verbose = Config.instance.verbose;
  
  private static String[] banned = {"[", "java/lang", "janala", "org/objectweb/asm", "sun", "jdk", "java/util/function"};
  private static String[] excludes;
  private static String[] includes;
  
  public static void premain(String agentArgs, Instrumentation inst) throws ClassNotFoundException {

    preloadClasses();

    excludes = Config.instance.excludeInst;
    includes = Config.instance.includeInst;
    inst.addTransformer(new SnoopInstructionTransformer(), true);
    if (inst.isRetransformClassesSupported()) {
      for (Class clazz : inst.getAllLoadedClasses()) {
        try {
          String cname = clazz.getName().replace(".","/");
          if (shouldExclude(cname) == false) {
            if (inst.isModifiableClass(clazz)) {
              inst.retransformClasses(clazz);
            } else {
              println("[JANALA] Could not instrument " + clazz + " :-(");
            }
          }
        } catch (Exception e){
          e.printStackTrace();
        }
      }
    }
  }

  private static void preloadClasses() throws ClassNotFoundException {
    Class.forName("java.util.ArrayDeque");
    Class.forName("java.util.LinkedList");
    Class.forName("java.util.LinkedList$Node");
    Class.forName("java.util.LinkedList$ListItr");
    Class.forName("java.util.TreeMap");
    Class.forName("java.util.TreeMap$Entry");
    Class.forName("java.util.zip.ZipFile");
    Class.forName("java.util.jar.JarFile");
  }

  /** packages that should be exluded from the instrumentation */
  private static boolean shouldExclude(String cname) {
    for (String e : banned) {
      if (cname.startsWith(e)) {
        return true;
      }
    }
    for (String e : includes) {
      if (cname.startsWith(e)) {
        return false;
      }
    }
    for (String e : excludes) {
      if (cname.startsWith(e)) {
        return true;
      }
    }
    return false;
  }

  static Map<String, byte[]> instrumentedBytes = new TreeMap<>();

  @Override
  synchronized public byte[] transform(ClassLoader loader, String cname, Class<?> classBeingRedefined,
      ProtectionDomain d, byte[] cbuf)
    throws IllegalClassFormatException {

    boolean toInstrument = !shouldExclude(cname);

    if (toInstrument) {
      print("[JANALA] ");
      if (classBeingRedefined != null) {
        print("* ");
      }
      print("Instrumenting: " + cname + "... ");
      GlobalStateForInstrumentation.instance.setCid(cname.hashCode());

      if (instrumentedBytes.containsKey(cname)) {
        println(" Found in fast-cache!");
        return instrumentedBytes.get(cname);
      }

      if (instDir != null) {
        File cachedFile = new File(instDir + "/" + cname + ".instrumented.class");
        File referenceFile = new File(instDir + "/" + cname + ".original.class");
        if (cachedFile.exists() && referenceFile.exists()) {
          try {
            byte[] origBytes = Files.readAllBytes(referenceFile.toPath());
            if (Arrays.equals(cbuf, origBytes)) {
              byte[] instBytes = Files.readAllBytes(cachedFile.toPath());
              println(" Found in disk-cache!");
              instrumentedBytes.put(cname, instBytes);
              return instBytes;
            }
          } catch (IOException e) {
            print(" <cache error> ");
          }
        }
      }


      ClassReader cr = new ClassReader(cbuf);
      ClassWriter cw = new SafeClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
      ClassVisitor cv = new SnoopInstructionClassAdapter(cw, cname);

      try {
        cr.accept(cv, 0);
      } catch (Throwable e) {
        e.printStackTrace();
        return null;
      }

      byte[] ret = cw.toByteArray();
      println("Done!");
      instrumentedBytes.put(cname, ret);

      if (instDir != null) {
        try {
          File cachedFile = new File(instDir + "/" + cname + ".instrumented.class");
          File referenceFile = new File(instDir + "/" + cname + ".original.class");
          File parent = new File(cachedFile.getParent());
          parent.mkdirs();
          try(FileOutputStream out = new FileOutputStream(cachedFile)) {
            out.write(ret);
          }
          try(FileOutputStream out = new FileOutputStream(referenceFile)) {
            out.write(cbuf);
          }
        } catch(Exception e) {
          e.printStackTrace();
        }
      }
      return ret;
    } else {
      return cbuf;
    }
  }

  private static void print(String str) {
    if (verbose) {
      System.out.print(str);
    }
  }

  private static void println(String line) {
    if (verbose) {
      System.out.println(line);
    }
  }
}
