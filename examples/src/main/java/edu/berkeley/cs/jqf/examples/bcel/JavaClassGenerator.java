/*
 * Copyright (c) 2018, The Regents of the University of California
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
package edu.berkeley.cs.jqf.examples.bcel;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Based on:
 * <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html">
 * Java Virtual Machine Specification 4
 * </a>
 */
public class JavaClassGenerator extends Generator<JavaClass> {
    private static final int MIN_FIELDS = 0;
    private static final int MAX_FIELDS = 10;
    private static final int MIN_INTERFACES = 0;
    private static final int MAX_INTERFACES = 5;
    private static final int MIN_METHODS = 0;
    private static final int MAX_METHODS = 10;
    private static final List<Consumer<ClassGen>> versionList =
            Collections.unmodifiableList(Arrays.asList((clazz) -> setVersion(clazz, Const.MAJOR_1_1, Const.MINOR_1_1),
                    (clazz) -> setVersion(clazz, Const.MAJOR_1_2, Const.MINOR_1_2),
                    (clazz) -> setVersion(clazz, Const.MAJOR_1_3, Const.MINOR_1_3),
                    (clazz) -> setVersion(clazz, Const.MAJOR_1_4, Const.MINOR_1_4),
                    (clazz) -> setVersion(clazz, Const.MAJOR_1_5, Const.MINOR_1_5),
                    (clazz) -> setVersion(clazz, Const.MAJOR_1_6, Const.MINOR_1_6),
                    (clazz) -> setVersion(clazz, Const.MAJOR_1_7, Const.MINOR_1_7),
                    (clazz) -> setVersion(clazz, Const.MAJOR_1_8, Const.MINOR_1_8),
                    (clazz) -> setVersion(clazz, Const.MAJOR_1_9, Const.MINOR_1_9)));
    private final Generator<String> classNameGenerator = new JavaClassNameGenerator(".");

    public JavaClassGenerator() {
        super(JavaClass.class);
    }

    public JavaClass generate(SourceOfRandomness random, GenerationStatus status) {
        ConstantPoolGen pool = new ConstantPoolGen();
        String className = classNameGenerator.generate(random, status);
        String superClassName = classNameGenerator.generate(random, status);
        String fileName = className.replace('.', '/') + ".class";
        ClassGen clazz = new ClassGen(className, superClassName, fileName, 0, new String[0], pool);
        setVersion(random, clazz);
        setAccessFlags(random, clazz);
        Stream.generate(() -> classNameGenerator.generate(random, status))
                .limit(random.nextInt(MIN_INTERFACES, MAX_INTERFACES))
                .forEach(clazz::addInterface);
        Stream.generate(new FieldGenerator(random, status, clazz)::generate)
                .limit(random.nextInt(MIN_FIELDS, MAX_FIELDS))
                .forEach(clazz::addField);
        Stream.generate(new MethodGenerator(random, status, clazz)::generate)
                .limit(random.nextInt(MIN_METHODS, MAX_METHODS))
                .forEach(clazz::addMethod);
        return clazz.getJavaClass();
    }

    static void setAccessFlags(SourceOfRandomness random, ClassGen clazz) {
        if (random.nextBoolean()) {
            clazz.isPublic(true);
        }
        if (random.nextBoolean()) {
            clazz.isSynthetic(true);
        }
        if (random.nextBoolean()) {
            // Abstract
            clazz.isAbstract(true);
            if (random.nextBoolean()) {
                clazz.isInterface(true);
                if (random.nextBoolean()) {
                    clazz.isAnnotation(true);
                }
            } else {
                if (random.nextBoolean()) {
                    clazz.isEnum(true);
                }
            }
        } else {
            // Concrete
            if (random.nextBoolean()) {
                clazz.isEnum(true);
            }
            if (random.nextBoolean()) {
                clazz.isFinal(true);
            }
        }
    }

    static void setVersion(SourceOfRandomness random, ClassGen clazz) {
        random.choose(versionList).accept(clazz);
    }

    static void setVersion(ClassGen clazz, int major, int minor) {
        clazz.setMajor(major);
        clazz.setMinor(minor);
    }
}


