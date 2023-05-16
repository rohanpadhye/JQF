package edu.berkeley.cs.jqf.examples.bcel;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import org.apache.bcel.generic.*;

import java.util.Arrays;
import java.util.List;

public class TypeGenerator {
    private final List<ObjectType> COMMON_TYPES =
            Arrays.asList(Type.OBJECT, Type.CLASS, Type.STRING, Type.STRINGBUFFER, Type.THROWABLE);
    private final List<BasicType> PRIMITIVE_TYPES =
            Arrays.asList(Type.BOOLEAN, Type.INT, Type.SHORT, Type.BYTE, Type.LONG, Type.DOUBLE, Type.FLOAT, Type.CHAR);
    private final Generator<String> classNameGenerator = new JavaClassNameGenerator("/");
    private final SourceOfRandomness random;
    private final GenerationStatus status;

    public TypeGenerator(SourceOfRandomness random, GenerationStatus status) {
        this.random = random;
        this.status = status;
    }

    public Type generate() {
        switch (random.nextInt(3)) {
            case 0:
                return generateArrayType();
            case 1:
                return generateObjectType();
            default:
                return generatePrimitiveType();
        }
    }

    public BasicType generatePrimitiveType() {
        return random.choose(PRIMITIVE_TYPES);
    }

    public ArrayType generateArrayType() {
        Type type = random.nextBoolean() ? generatePrimitiveType() : generateObjectType();
        return new ArrayType(type, random.nextInt(1, 10));
    }

    public ObjectType generateObjectType() {
        if (random.nextBoolean()) {
            return random.choose(COMMON_TYPES);
        } else {
            return new ObjectType(classNameGenerator.generate(random, status));
        }
    }

    public ReferenceType generateReferenceType() {
        return random.nextBoolean() ? generateArrayType() : generateObjectType();
    }
}
