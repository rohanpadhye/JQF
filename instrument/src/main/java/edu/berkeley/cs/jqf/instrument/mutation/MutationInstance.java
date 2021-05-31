/*
 * Copyright (c) 2021 Isabella Laybourn
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

import janala.instrument.SnoopInstructionTransformer;
import org.objectweb.asm.*;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.atomic.AtomicLong;

/**
 * mostly exported from InstrumentingClassLoader with additions to FindClass
 *
 * @author Bella Laybourn
 */
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
        defineClass("edu.berkeley.cs.jqf.instrument.mutation.MutationTimeoutException", bytes, 0, bytes.length);
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
                            if(opcode == Opcodes.IRETURN || opcode == Opcodes.ARETURN || opcode == Opcodes.RETURN) {
                                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "edu/berkeley/cs/jqf/instrument/mutation/MutationTimeoutException", "resetTimeout", "()V", false);
                            }
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
        }

        return defineClass(name, bytes, 0, bytes.length);
    }

    public void kill() {
        dead = true;
    }

    public boolean isDead() {
        return dead;
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
