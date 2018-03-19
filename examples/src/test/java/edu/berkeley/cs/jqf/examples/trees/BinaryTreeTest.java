package edu.berkeley.cs.jqf.examples.trees;

import java.util.Set;

import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.generator.Size;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.Assert;
import org.junit.runner.RunWith;

@RunWith(JQF.class)
public class BinaryTreeTest {


    @Fuzz
    public void insert(int @Size(min=100, max=100)[] elements) {
        BinaryTree b = new BinaryTree();
        for (int e : elements) {
            b.insert(e);
        }

    }

    @Fuzz
    public void contains(@Size(max=100) Set<@InRange(minInt=0, maxInt=100) Integer> elements, @InRange(minInt=0, maxInt=100) int @Size(max=10) [] queries) {
        BinaryTree b = new BinaryTree();
        for (Integer e : elements) {
            b.insert(e);

        }

        for (int q : queries) {
            Assert.assertEquals(elements.contains(q), b.contains(q));
        }
    }

}
