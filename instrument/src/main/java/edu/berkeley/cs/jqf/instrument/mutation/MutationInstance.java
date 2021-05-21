package edu.berkeley.cs.jqf.instrument.mutation;

import janala.instrument.SnoopInstructionTransformer;
import org.graalvm.compiler.nodes.calc.IntegerDivRemNode;
import org.objectweb.asm.*;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicLong;

/** mostly exported from InstrumentingClassLoader with additions to FindClass **/
public class MutationInstance extends URLClassLoader {

    /** which mutator to use */
    private final Mutator mutator;

    /** numbered instance of the opportunity for mutation this classloader uses */
    private final long instance;

    /** name of the class to mutate */
    private final String mutateName;

    /** see InstrumentingClassLoader */
    private final ClassFileTransformer transformer = new SnoopInstructionTransformer();

    /** whether this mutation has been killed already */
    private boolean dead;

    private final String className = this.getClass().getName();
    private static final int MAX_ITERATIONS = 100000;
    private static int jumps;

    //TODO potential for more information:
    //  line number
    //  who's seen it
    //  whether this mutation is likely to be killed by a particular input

    public MutationInstance(URL[] paths, ClassLoader parent, Mutator m, long i, String n, byte[] bytes) throws MalformedURLException, ClassNotFoundException {
        super(paths, parent);
        mutator = m;
        instance = i;
        mutateName = n;
        dead = false;
        jumps = 0;
        /*byte[] bytes;
        try (InputStream in = new FileInputStream(new File("target/edu/berkeley/cs/jqf/instrument/mutation/MutationTimeoutException.class"))) {
            if (in == null) {
                throw new ClassNotFoundException("Cannot find class MutationTimeoutException");
            }
            BufferedInputStream buf = new BufferedInputStream(in);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int b;
            while ((b = buf.read()) != -1) {
                baos.write(b);
            }
            bytes = baos.toByteArray();
        } catch (IOException e) {
            throw new ClassNotFoundException("I/O exception while loading class.", e);
        }*/
        defineClass("edu.berkeley.cs.jqf.instrument.mutation.MutationTimeoutException", bytes, 0, bytes.length);
        /*File outputFile = new File("MutationTimeoutException.class");
        try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
            outputStream.write(bytes);
        } catch(Exception e) {
            e.printStackTrace();
        }*/
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes;

        //System.out.println(super.findLoadedClass("edu.berkeley.cs.jqf.instrument.mutation.MutationTimeoutException"));

        String internalName = name.replace('.', '/');
        String path = internalName.concat(".class");
        try (InputStream in = super.getResourceAsStream(path)) {
            if (in == null) {
                throw new ClassNotFoundException("Cannot find class " + name);
            }
            BufferedInputStream buf = new BufferedInputStream(in);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int b;
            while ((b = buf.read()) != -1) {
                baos.write(b);
            }
            bytes = baos.toByteArray();
        } catch (IOException e) {
            throw new ClassNotFoundException("I/O exception while loading class.", e);
        }

        //System.out.println("act name: " + name);
        /*System.out.println("defined already? " + super.findLoadedClass(name));
        boolean selfLoaded = super.findLoadedClass(className) != null;
        if(!selfLoaded) {
            super.loadClass(className);
        }*/

        if(name.equals(mutateName)) {
            AtomicLong found = new AtomicLong(0);
            ClassWriter cw = new ClassWriter(0);
            ClassReader cr = new ClassReader(bytes);
            cr.accept(new ClassVisitor(Mutator.cvArg, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String signature,
                                                 String superName, String[] interfaces) {
                    return new MethodVisitor(Mutator.cvArg, cv.visitMethod(access, name,
                            signature, superName, interfaces)) {
                        @Override
                        public void visitJumpInsn(int opcode, Label label) {
                            /*System.out.println("class name: " + className);

                            byte[] xbytes = new byte[0];
                            try (InputStream in = getClass().getClassLoader().getResourceAsStream("edu/berkeley/cs/jqf/instrument/mutation/MutationTimeoutException.class")) {
                                BufferedInputStream buf = new BufferedInputStream(in);

                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                int b;
                                while ((b = buf.read()) != -1) {
                                    baos.write(b);
                                }
                                xbytes = baos.toByteArray();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            File outputFile = new File("MutationTimeoutException.class");
                            try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                                outputStream.write(xbytes);
                            } catch(Exception e) {
                                e.printStackTrace();
                            }*/
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "edu/berkeley/cs/jqf/instrument/mutation/MutationTimeoutException", "checkTimeout", "()V", false);
                            if (mutator.isOpportunity(opcode, signature) && found.get() == instance) {
                                for (InstructionCall ic : mutator.replaceWith(opcode, signature)) {
                                    ic.call(mv, label);
                                }
                                found.getAndIncrement();
                            } else if (mutator.isOpportunity(opcode, signature)) {
                                super.visitJumpInsn(opcode, label);
                                found.getAndIncrement();
                            } else {
                                super.visitJumpInsn(opcode, label);
                            }
                        }

                        @Override
                        public void visitLdcInsn(Object value) {
                            if (mutator.isOpportunity(Opcodes.LDC, signature) && found.get() == instance) {
                                for (InstructionCall ic : mutator.replaceWith(Opcodes.LDC, signature)) {
                                    ic.call(mv, null);
                                }
                                found.getAndIncrement();
                            } else if (mutator.isOpportunity(Opcodes.LDC, signature)) {
                                super.visitLdcInsn(value);
                                found.getAndIncrement();
                            } else {
                                super.visitLdcInsn(value);
                            }
                        }
                        @Override
                        public void visitIincInsn(int var, int increment) {
                            if (mutator.isOpportunity(Opcodes.IINC, signature) && found.get() == instance) {
                                super.visitIincInsn(var, -increment);
                                found.getAndIncrement();
                            } else if (mutator.isOpportunity(Opcodes.IINC, signature)) {
                                super.visitIincInsn(var, increment);
                                found.getAndIncrement();
                            } else {
                                super.visitIincInsn(var, increment);
                            }
                        }
                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                            if (mutator.isOpportunity(opcode, descriptor) && found.get() == instance) {
                                for (InstructionCall ic : mutator.replaceWith(opcode, descriptor)) {
                                    ic.call(mv, null);
                                }
                                found.getAndIncrement();
                            } else if (mutator.isOpportunity(opcode, descriptor)) {
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                                found.getAndIncrement();
                            } else {
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            }
                        }
                        @Override
                        public void visitInsn(int opcode) {
                            if (mutator.isOpportunity(opcode, signature) && found.get() == instance) {
                                for (InstructionCall ic : mutator.replaceWith(opcode, signature)) {
                                    ic.call(mv, null);
                                }
                                found.getAndIncrement();
                            } else if (mutator.isOpportunity(opcode, signature)) {
                                super.visitInsn(opcode);
                                found.getAndIncrement();
                            } else {
                                super.visitInsn(opcode);
                            }
                        }
                    };
                }
            }, 0);
            bytes = cw.toByteArray();
            //System.out.println(cr.getClassName());
        }

        //System.out.println("name: ");
        Class<?> clazz = defineClass(name, bytes, 0, bytes.length);
        //System.out.println(clazz.getName());

        return clazz;
    }

    public void kill() {
        dead = true;
    }

    public boolean isDead() {
        return dead;
    }

    /**
     * Insert calls to this as instrumentation in the program
     * (see line 81)
     */
    public static void timeoutCheck() throws MutationTimeoutException {
        System.out.println("timeout check!");
        if(++jumps > MAX_ITERATIONS) {
            throw new MutationTimeoutException();
        }
    }

    public void resetTimeout() {
        jumps = 0;
    }

    @Override
    public String toString() {
        String toReturn = "MutationInstance " + instance + " of " + mutator + " in " + mutateName + " (";
        if(dead) {
            return toReturn + "dead)";
        } else {
            return toReturn + "alive)";
        }
    }
}
