package edu.berkeley.cs.jqf.instrument.mutation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * A {@link ClassWriter} which takes as input a {@link ClassReader},
 * instrumenting it to collect data about what mutation opportunities are
 * available, and which run during the intial "exploration".
 */
public class Cartographer extends ClassVisitor {
    /** The set of mutant opportunities */
    private Map<Mutator, List<MutationInstance>> opportunities;

    /** The API Version */
    private static final int API = Opcodes.ASM8;

    /** The name of the class we're visitng */ 
    private String name = null;

    /**
     * Creates a cartographer
     */
    public Cartographer() {
        super(API);
        init();
    }

    /**
     * Creates a Cartographer for a specific {@link ClassReader}, which allows for
     * optimization
     * 
     * @param classReader the class which will be instrumented
     */
    public Cartographer(ClassReader classReader) {
        super(API, new ClassWriter(classReader, 0));
        init();
    }

    /**
     * Initializes the {@code opportunities} and {@code occurances}
     */
    private void init() {
        // I can't just add this to one of the constructors
        opportunities = new HashMap<>(Mutator.values().length);

        for (Mutator mutator : Mutator.values())
            opportunities.put(mutator, new ArrayList<>());
    }

    /**
     * Creates a cartographer specialized on a {@link ClassReader}, then runs it on
     * that {@link ClassReader}, returning the cartographer.
     * 
     * @param r a reader for reading the class in question
     * @return the cartographer which has walked the class
     */
    public static Cartographer explore(ClassReader r) {
        Cartographer c = new Cartographer(r);
        r.accept(c, 0);
        return c;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        // Change from the ASM's internal naming scheme to the package naming scheme
        this.name = name.replace("/", ".");
        super.visit(version, access, name, signature, superName, interfaces);
    }

    /**
     * Gets the possible opportunities the mutators
     *
     * @return the opportunities for each mutator
     */
    public Map<Mutator, List<MutationInstance>> getOpportunities() {
        return opportunities;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
            String[] exceptions) {
        return new MethodVisitor(API, cv.visitMethod(access, name, descriptor, signature, exceptions)) {

            /**
             * Logs that a mutator can be used at the current location in the tree.
             *
             * @param mut The mutator to be logged
             */
            private void logMutOp(Mutator mut) {
                List<MutationInstance> ops = opportunities.get(mut);
                ops.add(new MutationInstance(mut, ops.size(), Cartographer.this.name));

                // TODO: Add instrumentation
            }

            /**
             * Checks if the opcode/descriptor could be the target of a mutation
             * 
             * @param opcode     The opcode of the mutation to be performed
             * @param descriptor The descriptor of the method, if it has one
             */
            private void check(int opcode, String descriptor) {
                for (Mutator m : Mutator.values())
                    if (m.isOpportunity(opcode, descriptor))
                        logMutOp(m);
            }

            /**
             * Checks if the opcode could be the target of a mutant
             *
             * @param opcode The opcode of the instruction
             */
            private void check(int opcode) {
                check(opcode, descriptor);
            }

            // TODO: Remove this duplication, somehow

            @Override
            public void visitJumpInsn(int opcode, Label label) {
                check(opcode);
                super.visitJumpInsn(opcode, label);
            }

            @Override
            public void visitLdcInsn(Object value) {
                check(Opcodes.LDC);
                super.visitLdcInsn(value);
            }

            @Override
            public void visitIincInsn(int var, int inc) {
                check(Opcodes.IINC);
                super.visitIincInsn(var, inc);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                check(opcode, descriptor);
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }

            @Override
            public void visitInsn(int opcode) {
                check(opcode);
                super.visitInsn(opcode);
            }
        };
    };

    byte[] toByteArray() {
        return ((ClassWriter) cv).toByteArray();
    }
}
