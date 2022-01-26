package edu.berkeley.cs.jqf.examples.simple;

import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.runner.RunWith;

@RunWith(JQF.class)
public class SimpleClassTest {

    public static class SimpleGenerator extends Generator<Integer> {
        public SimpleGenerator() {
            super(Integer.class);
        }

        @Override
        public Integer generate(SourceOfRandomness sourceOfRandomness, GenerationStatus generationStatus) {
            return sourceOfRandomness.nextInt();
        }
    }

    @Fuzz
    public void testWithGenerator(@From(SimpleGenerator.class) Integer a) {
        SimpleClass.test(a);
    }
}