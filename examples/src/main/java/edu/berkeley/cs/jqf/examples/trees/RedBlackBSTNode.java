package edu.berkeley.cs.jqf.examples.trees;

/**
 * Created by clemieux on 9/18/17.
 */
public class RedBlackBSTNode<Key,Value> {

    public Key key;           // key
    public Value val;         // associated data
    public RedBlackBSTNode<Key,Value> left, right;  // links to left and right subtrees
    public boolean color;     // color of parent link
    public int size;          // subtree count

    public RedBlackBSTNode(Key key, Value val, boolean color, int size) {
        this.key = key;
        this.val = val;
        this.color = color;
        this.size = size;
    }
}
