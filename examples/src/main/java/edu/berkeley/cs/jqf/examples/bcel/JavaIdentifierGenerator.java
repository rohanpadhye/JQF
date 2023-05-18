package edu.berkeley.cs.jqf.examples.bcel;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.Size;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import static com.pholser.junit.quickcheck.internal.Ranges.Type.INTEGRAL;
import static com.pholser.junit.quickcheck.internal.Ranges.checkRange;

public final class JavaIdentifierGenerator extends Generator<String> {
    private static final char MIN_VALID_CHAR = '$';
    private static final char MAX_VALID_CHAR = 'z';
    private int minSize = 1;
    private int maxSize = 30;

    public JavaIdentifierGenerator() {
        super(String.class);
    }

    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        return generate(random);
    }

    @SuppressWarnings("unused")
    public void configure(Size size) {
        if (size.min() <= 0) {
            throw new IllegalArgumentException("Minimum size must be positive");
        }
        checkRange(INTEGRAL, size.min(), size.max());
        minSize = size.min();
        maxSize = size.max();
    }

    public String generate(SourceOfRandomness random) {
        int size = random.nextInt(minSize, maxSize);
        char[] values = new char[size];
        values[0] = generateJavaIdentifierStart(random);
        for (int i = 1; i < values.length; i++) {
            values[i] = generateJavaIdentifierPart(random);
        }
        return new String(values);
    }

    static char generateJavaIdentifierStart(SourceOfRandomness random) {
        char c = random.nextChar(MIN_VALID_CHAR, MAX_VALID_CHAR);
        if (!Character.isJavaIdentifierStart(c)) {
            return mapToAlpha(c);
        }
        return c;
    }

    static char generateJavaIdentifierPart(SourceOfRandomness random) {
        char c = random.nextChar(MIN_VALID_CHAR, MAX_VALID_CHAR);
        if (!Character.isJavaIdentifierPart(c)) {
            return mapToAlpha(c);
        }
        return c;
    }

    static char mapToAlpha(char c) {
        char min = 'a';
        char max = 'z';
        int range = max - min + 1;
        return (char) ((c % range) + min);
    }
}
