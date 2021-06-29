package edu.berkeley.cs.jqf.instrument.mutation;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicLong;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Classloader that loads test classes and performs exactly one mutation.
 *
 * Mostly exported from InstrumentingClassLoader with additions to FindClass.
 *
 * @author Bella Laybourn
 */
public class MutationClassLoader extends URLClassLoader {

    /**
     * The mutation instance this class loads
     */
    private final MutationInstance mutationInstance;

    public MutationClassLoader(MutationInstance mutationInstance, URL[] paths, ClassLoader parent) {
        super(paths, parent);
        this.mutationInstance = mutationInstance;
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
            bytes = in.readAllBytes();
        } catch (IOException e) {
            throw new ClassNotFoundException("I/O exception while loading class.", e);
        }

        if (name.equals(this.mutationInstance.className)) {
            AtomicLong found = new AtomicLong(0);
            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            ClassReader cr = new ClassReader(bytes);
            cr.accept(new ClassVisitor(Mutator.cvArg, cw) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String signature, String superName,
                        String[] interfaces) {
                    return new MethodVisitor(Mutator.cvArg,
                            cv.visitMethod(access, name, signature, superName, interfaces)) {
                        @Override
                        public void visitJumpInsn(int opcode, Label label) {
                            // Increment timer and check for time outs at each jump instruction
                            mv.visitLdcInsn(mutationInstance.id);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                                    "edu/berkeley/cs/jqf/instrument/mutation/MutationSnoop", "checkTimeout", "(I)V",
                                    false);

                            // Increment offset if the mutator matches
                            if (MutationClassLoader.this.mutationInstance.mutator.isOpportunity(opcode, signature)
                                    && found.getAndIncrement() == MutationClassLoader.this.mutationInstance.mutatorOffsetWithinClass) {
                                // Mutator and offset match, so perform mutation
                                for (InstructionCall ic : MutationClassLoader.this.mutationInstance.mutator.replaceWith()) {
                                    ic.call(mv, label);
                                }
                            } else {
                                // No mutation
                                super.visitJumpInsn(opcode, label);
                            }
                        }

                        @Override
                        public void visitLdcInsn(Object value) {
                            // Increment offset if the mutator matches
                            if (MutationClassLoader.this.mutationInstance.mutator.isOpportunity(Opcodes.LDC, signature)
                                    && found.getAndIncrement() == MutationClassLoader.this.mutationInstance.mutatorOffsetWithinClass) {
                                // Mutator and offset match, so perform mutation
                                for (InstructionCall ic : MutationClassLoader.this.mutationInstance.mutator.replaceWith()) {
                                    ic.call(mv, null);
                                }
                            } else {
                                // No mutation
                                super.visitLdcInsn(value);
                            }
                        }

                        @Override
                        public void visitIincInsn(int var, int increment) {
                            // Increment offset if the mutator matches
                            if (MutationClassLoader.this.mutationInstance.mutator.isOpportunity(Opcodes.IINC, signature)
                                    && found.getAndIncrement() == MutationClassLoader.this.mutationInstance.mutatorOffsetWithinClass) {
                                // Mutator and offset match, so perform mutation
                                super.visitIincInsn(var, -increment); // TODO: Why is this hardcoded?
                            } else {
                                // No mutation
                                super.visitIincInsn(var, increment);
                            }
                        }

                        @Override
                        public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
                                boolean isInterface) {
                            // Increment offset if the mutator matches
                            if (MutationClassLoader.this.mutationInstance.mutator.isOpportunity(opcode, descriptor)
                                    && found.getAndIncrement() == MutationClassLoader.this.mutationInstance.mutatorOffsetWithinClass) {
                                // Mutator and offset match, so perform mutation
                                for (InstructionCall ic : MutationClassLoader.this.mutationInstance.mutator.replaceWith()) {
                                    ic.call(mv, null);
                                }
                            } else {
                                // No mutation
                                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                            }
                        }

                        @Override
                        public void visitInsn(int opcode) {
                            // Increment offset if the mutator matches
                            if (MutationClassLoader.this.mutationInstance.mutator.isOpportunity(opcode, signature)
                                    && found.getAndIncrement() == MutationClassLoader.this.mutationInstance.mutatorOffsetWithinClass) {
                                // Mutator and offset match, so perform mutation
                                for (InstructionCall ic : MutationClassLoader.this.mutationInstance.mutator.replaceWith()) {
                                    ic.call(mv, null);
                                }
                            } else {
                                // No mutation
                                super.visitInsn(opcode);
                            }
                        }
                    };
                }
            }, 0);
            bytes = cw.toByteArray();
        }

        return defineClass(name, bytes, 0, bytes.length);
    }
}
