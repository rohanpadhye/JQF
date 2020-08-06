package edu.berkeley.cs.jqf.examples.xml;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

public class XmlStringIterativeGenerator extends Generator<String> {
    public XmlStringIterativeGenerator() {
        super(String.class);
    }

    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        XmlDocumentIterativeGenerator generator = new XmlDocumentIterativeGenerator();
        return XMLDocumentUtils.documentToString(generator.generate(random, status));
    }
}
