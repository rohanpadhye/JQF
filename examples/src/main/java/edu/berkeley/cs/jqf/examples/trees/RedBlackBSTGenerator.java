package edu.berkeley.cs.jqf.examples.trees;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import static com.pholser.junit.quickcheck.internal.Reflection.defaultValueOf;

/**
 * Created by clemieux on 9/18/17.
 */
public class RedBlackBSTGenerator extends Generator<RedBlackBST> {
    private int min = (Integer) defaultValueOf(InRange.class, "minInt");
    private int max = (Integer) defaultValueOf(InRange.class, "maxInt");

    private static final GenerationStatus.Key<Integer> SUBTREE_SIZE = new GenerationStatus.Key<>("treesize", Integer.class);

    public RedBlackBSTGenerator() {
        super(RedBlackBST.class);
    }

    /**
     * Tells this generator to produce values within a specified minimum and/or
     * maximum, inclusive, with uniform distribution.
     *
     * {@link InRange#min} and {@link InRange#max} take precedence over
     * {@link InRange#minInt()} and {@link InRange#maxInt()}, if non-empty.
     *
     * @param range annotation that gives the range's constraints
     */
    public void configure(InRange range) {
        min = range.min().isEmpty() ? range.minInt() : Integer.parseInt(range.min());
        max = range.max().isEmpty() ? range.maxInt() : Integer.parseInt(range.max());
    }

    @Override
    public RedBlackBST<Integer,Integer> generate(SourceOfRandomness sourceOfRandomness, GenerationStatus generationStatus) {
        Integer size = sourceOfRandomness.nextInt(min,max);
       // System.out.println("size: " + size.toString());
        generationStatus.setValue(SUBTREE_SIZE, size);
        RedBlackBSTNodeGenerator generator = new RedBlackBSTNodeGenerator();
        return new RedBlackBST<>(generator.generate(sourceOfRandomness, generationStatus));
    }
}
