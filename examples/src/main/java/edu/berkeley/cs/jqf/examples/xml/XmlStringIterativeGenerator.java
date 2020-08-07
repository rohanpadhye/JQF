package edu.berkeley.cs.jqf.examples.xml;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.examples.common.Dictionary;

import java.io.IOException;

public class XmlStringIterativeGenerator extends Generator<String> {
    private XmlDocumentIterativeGenerator generator;

    public XmlStringIterativeGenerator() {
        super(String.class);
        generator = new XmlDocumentIterativeGenerator();
    }

    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        return XMLDocumentUtils.documentToString(this.generator.generate(random, status));
    }

    public void configure(Dictionary dict) throws IOException {
        this.generator.configure(dict);
    }
}
