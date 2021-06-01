/*
 * Copyright (c) 2021 Isabella Laybourn, Rohan Padhye
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.berkeley.cs.jqf.instrument.mutation;

import org.objectweb.asm.*;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;


public class MutationInstance {

    /** Globally unique identifier for this mutation instance */
    public final int id;

    /** Static list of all registered mutation instances. */
    private static final ArrayList<MutationInstance> mutationInstances = new ArrayList<>();

    /** The type of mutation represented by a mutator */
    private final Mutator mutator;

    /** Name of the class to mutate */
    private final String className;

    /** Numbered instance of the opportunity for mutation this classloader uses */
    private final long mutatorOffsetWithinClass;

    /** The classloader for this mutation instance. */
    private final MutationClassLoader classLoader;

    /** Whether this mutation has been killed already */
    private boolean dead; // TODO: Move this to guidance

    /** Counter that is incremented during execution of this mutation instance to catch infinite loops. */
    private long timeoutCounter = 0;

    //TODO potential for more information:
    //  line number
    //  who's seen it
    //  whether this mutation is likely to be killed by a particular input

    public MutationInstance(URL[] paths, ClassLoader parent, Mutator m, long i, String n) {
        this.id = mutationInstances.size();
        this.className = n;
        this.mutator = m;
        this.mutatorOffsetWithinClass = i;
        this.classLoader = new MutationClassLoader(paths, parent);
        this.dead = false;

        // Register mutation instance
        mutationInstances.add(this);
    }

    /**
     * Get the class loader associated with this mutation instance.
     *
     * @return a classloader that performs exactly one mutation
     */
    public MutationClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Classloader that loads test classes and performs exactly one mutation.
     *
     * Mostly exported from InstrumentingClassLoader with additions to FindClass.
     *
     * @author Bella Laybourn
     */
    public class MutationClassLoader extends URLClassLoader {

        public MutationClassLoader(URL[] paths, ClassLoader parent) {
            super(paths, parent);
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

            if(name.equals(className)) {
                AtomicLong found = new AtomicLong(0);
                ClassWriter cw = new ClassWriter(
                        ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                ClassReader cr = new ClassReader(bytes);
                cr.accept(new ClassVisitor(Mutator.cvArg, cw) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String signature,
                                                     String superName, String[] interfaces) {
                        return new MethodVisitor(Mutator.cvArg, cv.visitMethod(access, name,
                                signature, superName, interfaces)) {
                            @Override
                            public void visitJumpInsn(int opcode, Label label) {
                                // Increment timer and check for time outs at each jump instruction
                                mv.visitLdcInsn(MutationInstance.this.id);
                                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "edu/berkeley/cs/jqf/instrument/mutation/MutationSnoop", "checkTimeout", "(I)V", false);

                                if (mutator.isOpportunity(opcode, signature) && found.get() == mutatorOffsetWithinClass) {
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
                                if (mutator.isOpportunity(Opcodes.LDC, signature) && found.get() == mutatorOffsetWithinClass) {
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
                                if (mutator.isOpportunity(Opcodes.IINC, signature) && found.get() == mutatorOffsetWithinClass) {
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
                                if (mutator.isOpportunity(opcode, descriptor) && found.get() == mutatorOffsetWithinClass) {
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
                                if (mutator.isOpportunity(opcode, signature) && found.get() == mutatorOffsetWithinClass) {
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
            }

            return defineClass(name, bytes, 0, bytes.length);
        }
    }


    public void kill() {
        dead = true;
    }

    public boolean isDead() {
        return dead;
    }

    public void resetTimer() {
        this.timeoutCounter = 0;
    }

    public long getTimeoutCounter() {
        return this.timeoutCounter;
    }

    public long incrementTimeoutCounter() {
        return ++this.timeoutCounter;
    }

    @Override
    public String toString() {
        return String.format("%s::%s::%d", className, mutator, mutatorOffsetWithinClass);
    }

    public static MutationInstance getInstance(int id) {
        return mutationInstances.get(id);
    }
}
