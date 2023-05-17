package edu.berkeley.cs.jqf.examples.trees;

import java.util.Optional;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

/**
 * Created by clemieux on 9/15/17.
 */
public class RedBlackBSTNodeGenerator extends Generator<RedBlackBSTNode> {

    private static final GenerationStatus.Key<Integer> SUBTREE_SIZE = new GenerationStatus.Key<>("treesize", Integer.class);

    public RedBlackBSTNodeGenerator() {
        super(RedBlackBSTNode.class);
    }

    @Override public RedBlackBSTNode<Integer,Integer> generate(SourceOfRandomness r, GenerationStatus status){
        Optional<Integer> treesizeOpt = status.valueOf(SUBTREE_SIZE);
        if (!treesizeOpt.isPresent()) return null;
        Integer treesize = treesizeOpt.get();
        if (treesize == 0 ){
            return null;
        }
        treesize = treesize -1;
        int leftSize = r.nextInt(0, treesize);
        int rightSize = treesize - leftSize;
        //System.out.println("leftSize: " + leftSize + ", rightSize: " + rightSize);


        RedBlackBSTNode<Integer,Integer> retNode = new RedBlackBSTNode<>(r.nextInt(), r.nextInt(), r.nextBoolean(), treesize + 1);
     //   System.out.println(retNode.key);
        retNode.left= generate(r, status.setValue(SUBTREE_SIZE, leftSize));
        retNode.right = generate(r, status.setValue(SUBTREE_SIZE, rightSize));
        return retNode;
    }
}
