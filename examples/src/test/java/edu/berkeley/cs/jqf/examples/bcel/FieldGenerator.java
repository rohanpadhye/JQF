package edu.berkeley.cs.jqf.examples.bcel;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.java.lang.StringGenerator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.AccessFlags;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.FieldGen;
import org.apache.bcel.generic.Type;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Based on:
 * <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html">
 * Java Virtual Machine Specification 4
 * </a>
 */
public final class FieldGenerator {
    private static final List<Consumer<? super AccessFlags>> VISIBILITY_SETTERS =
            Arrays.asList((f) -> f.isPrivate(true), (f) -> f.isPublic(true), (f) -> f.isProtected(true), (f) -> {
            });
    private final Generator<String> identifierGenerator = new JavaIdentifierGenerator();
    private final Generator<String> stringConstantGenerator = new StringGenerator();
    private final TypeGenerator typeGenerator;
    private final SourceOfRandomness random;
    private final GenerationStatus status;
    private final ClassGen clazz;

    public FieldGenerator(SourceOfRandomness random, GenerationStatus status, ClassGen clazz) {
        this.random = random;
        this.status = status;
        this.clazz = clazz;
        this.typeGenerator = new TypeGenerator(random, status);
    }

    public Field generate() {
        Type type = typeGenerator.generate();
        String name = identifierGenerator.generate(random, status);
        FieldGen field = new FieldGen(0, type, name, clazz.getConstantPool());
        setAccessFlags(field);
        if (field.isFinal()) {
            setInitValue(type, field);
        }
        return field.getField();
    }

    private void setInitValue(Type type, FieldGen field) {
        if (type instanceof BasicType) {
            if (random.nextBoolean()) {
                switch (type.getType()) {
                    case Const.T_BOOLEAN:
                        field.setInitValue(random.nextBoolean());
                        break;
                    case Const.T_BYTE:
                        field.setInitValue(random.nextByte(Byte.MIN_VALUE, Byte.MAX_VALUE));
                        break;
                    case Const.T_SHORT:
                        field.setInitValue(random.nextShort(Short.MIN_VALUE, Short.MAX_VALUE));
                        break;
                    case Const.T_CHAR:
                        field.setInitValue(random.nextChar(Character.MIN_VALUE, Character.MAX_VALUE));
                        break;
                    case Const.T_INT:
                        field.setInitValue(random.nextInt());
                        break;
                    case Const.T_LONG:
                        field.setInitValue(random.nextLong());
                        break;
                    case Const.T_DOUBLE:
                        field.setInitValue(random.nextDouble());
                        break;
                    case Const.T_FLOAT:
                        field.setInitValue(random.nextFloat());
                        break;
                }
            }
        } else if (type.equals(Type.STRING)) {
            if (random.nextBoolean()) {
                field.setInitValue(stringConstantGenerator.generate(random, status));
            }
        }
    }

    void setAccessFlags(FieldGen field) {
        if (random.nextBoolean()) {
            field.isSynthetic(true);
        }
        if (clazz.isInterface()) {
            field.isPublic(true);
            field.isStatic(true);
            field.isFinal(true);
        } else {
            random.choose(VISIBILITY_SETTERS).accept(field);
            if (random.nextBoolean()) {
                field.isStatic(true);
            }
            if (random.nextBoolean()) {
                field.isTransient(true);
            }
            if (random.nextBoolean()) {
                field.isEnum(true);
            }
            switch (random.nextInt(3)) {
                case 0:
                    field.isFinal(true);
                    break;
                case 1:
                    field.isVolatile(true);
            }
        }
    }
}
