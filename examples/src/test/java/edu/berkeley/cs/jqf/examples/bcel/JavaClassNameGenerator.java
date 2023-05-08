package edu.berkeley.cs.jqf.examples.bcel;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

public class JavaClassNameGenerator extends Generator<String> {
    private static final String[] BASIC_CLASS_NAMES = {"java/lang/Object",
            "java/util/List",
            "java/util/Map",
            "java/lang/String",
            "example/A",
            "example/B",
            "java/lang/Throwable",
            "java/lang/RuntimeException"};
    private final Generator<String> identifierGenerator = new JavaIdentifierGenerator();
    private final String delimiter;

    public JavaClassNameGenerator() {
        this("/");
    }

    public JavaClassNameGenerator(String delimiter) {
        super(String.class);
        this.delimiter = delimiter;
    }

    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        if (random.nextBoolean()) {
            return random.choose(BASIC_CLASS_NAMES);
        }
        String[] parts = new String[random.nextInt(1, 5)];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = identifierGenerator.generate(random, status);
        }
        return String.join(delimiter, parts);
    }
}
