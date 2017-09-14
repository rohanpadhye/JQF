package benchmarks;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import com.pholser.junit.quickcheck.Mode;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.generator.Size;
import com.pholser.junit.quickcheck.guided.GuidedJunitQuickcheckTest;
import org.junit.Assert;

/**
 * @author Rohan Padhye
 */
public class BinaryTreeTest extends GuidedJunitQuickcheckTest {


    @Property
    public void insert(LinkedList<Integer> elements) {
        BinaryTree b = new BinaryTree();
        for (Integer e : elements) {
            b.insert(e);
        }

        int uniqueElements = new HashSet<>(elements).size();
        Assert.assertEquals(uniqueElements, b.size());

    }

    @Property(mode= Mode.GUIDED, shrink=false, trials=300_000)
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