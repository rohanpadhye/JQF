package edu.berkeley.cs.jqf.instrument.mutation;

import janala.instrument.SnoopInstructionTransformer;
import org.objectweb.asm.*;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ClassLoader for initial run in mutation guidance
 * Runs like InstrumentingClassLoader while also prepping MutationInstances
 */
public class CartographyClassLoader extends URLClassLoader {
    /** List of available MutationInstances */
    private final List<MutationInstance> cartograph;

    /** if nonempty, class must be here to be mutable */
    private final List<String> includeClasses;

    /** class must not be here to be mutable */
    private final List<String> excludeClasses;

    /** see InstrumentingClassLoader */
    private final ClassFileTransformer transformer = new SnoopInstructionTransformer();

    /** Constructor */
    public CartographyClassLoader(String[] paths, String[] mutables, String[] immutables, ClassLoader parent) throws MalformedURLException {
        super(stringsToUrls(paths), parent);
        includeClasses = new ArrayList<>(Arrays.asList(mutables));
        excludeClasses = new ArrayList<>(Arrays.asList(immutables));
        cartograph = new ArrayList<>();
    }

    /** see InstrumentingClassLoader */
    public static URL[] stringsToUrls(String[] paths) throws MalformedURLException {
        URL[] urls = new URL[paths.length];
        for (int i = 0; i < paths.length; i++) {
            urls[i] = new File(paths[i]).toURI().toURL();
        }
        return urls;
    }

    public List<MutationInstance> getCartograph() {
        return cartograph;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes;

        String internalName = name.replace('.', '/');
        String path = internalName.concat(".class");
        try (InputStream in = super.getResourceAsStream(path)) {
            if (in == null) {
                throw new ClassNotFoundException("Cannot find class " + name);
            }
            bytes = getBytes(in);
        } catch (IOException e) {
            throw new ClassNotFoundException("I/O exception while loading class.", e);
        }

        // Check includes + excludes
        boolean mutable = includeClasses.isEmpty();
        for(String s : includeClasses) {
            if (name.startsWith(s)) {
                mutable = true;
                break;
            }
        }
        for(String s : excludeClasses) {
            if (name.startsWith(s)) {
                mutable = false;
                break;
            }
        }

        // Make cartograph
        if(mutable) {
            for(Mutator m : Mutator.values()) {
                long instances = getInstanceCount(bytes, m);
                for(int c = 0; c < instances; c++) {
                    try {
                        cartograph.add(new MutationInstance(getURLs(), getParent(), m, c, name));
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        // Instrument class to run like InstrumentingClassLoader
        byte[] transformedBytes;
        try {
            transformedBytes = transformer.transform(this, internalName, null, null, bytes);
        } catch (IllegalClassFormatException e) {
            // Just use original bytes
            transformedBytes = null;
        }

        // Load the class with transformed bytes, if possible
        if (transformedBytes != null) {
            bytes = transformedBytes;
        }

        return defineClass(name, bytes, 0, bytes.length);
    }

    /** see InstrumentingClassLoader */
    private byte[] getBytes(InputStream in) throws IOException {
        BufferedInputStream buf = new BufferedInputStream(in);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b;
        while ((b = buf.read()) != -1) {
            baos.write(b);
        }
        return baos.toByteArray();
    }

    /** get number of opportunities to apply mutator in the class described by bytes */
    private long getInstanceCount(byte[] bytes, Mutator mutator) {
        AtomicLong instances = new AtomicLong(0);
        ClassWriter cw = new ClassWriter(0);
        ClassReader cr = new ClassReader(bytes);
        ClassVisitor cv = new ClassVisitor(Mutator.cvArg, cw) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String signature,
                                             String superName, String[] interfaces) {
                return new MethodVisitor(Mutator.cvArg, cv.visitMethod(access, name,
                        signature, superName, interfaces)) {
                    @Override
                    public void visitJumpInsn(int opcode, Label label) {
                        if (mutator.isOpportunity(opcode, signature)) {
                            instances.getAndIncrement();
                        }
                        super.visitJumpInsn(opcode, label);
                    }
                    @Override
                    public void visitLdcInsn(Object value) {
                        if (mutator.isOpportunity(Opcodes.LDC, signature)) {
                            instances.getAndIncrement();
                        }
                        super.visitLdcInsn(value);
                    }
                    @Override
                    public void visitIincInsn(int var, int increment) {
                        if (mutator.isOpportunity(Opcodes.IINC, signature)) {
                            instances.getAndIncrement();
                        }
                        super.visitIincInsn(var, increment);
                    }
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                        if (mutator.isOpportunity(opcode, descriptor)) {
                            instances.getAndIncrement();
                        }
                        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                    }
                    @Override
                    public void visitInsn(int opcode) {
                        if (mutator.isOpportunity(opcode, signature)) {
                            instances.getAndIncrement();
                        }
                        super.visitInsn(opcode);
                    }
                };
            }
        };
        cr.accept(cv, 0);
        return instances.longValue();
    }
}
