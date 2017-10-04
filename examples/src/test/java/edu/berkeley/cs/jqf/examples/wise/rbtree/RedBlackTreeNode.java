/**
 * This code originally by Tuomo Saarni.  Obtained from:
 *
 *     http://users.utu.fi/~tuiisa/Java/index.html
 *
 * under the following license:
 *
 *     Here's some java sources I've made. Most codes are free to
 *     download. If you use some of my sources just remember give me
 *     the credits.
 */

package edu.berkeley.cs.jqf.examples.wise.rbtree;

/**
 * A <code>RedBlackTreeNode</code> object is a node of a Red-Black
 * tree. It extends the <code>Node</code> class with variables color
 * parent, left child and right child.
 * <p/>
 *
 * @author Tuomo Saarni
 * @version 1.1, 08/16/01
 */

public class RedBlackTreeNode extends Node {
    /* Variables */


    /**
     * The color node of current node.
     *
     * @see #setBlack()
     * @see #isBlack()
     * @see #setRed()
     * @see #isRed()
     */
    protected String color;

    /**
     * The parent node of current node.
     *
     * @see #parent()
     * @see #parentTo
     */
    protected RedBlackTreeNode p;

    /**
     * The left child node of current node.
     *
     * @see #left()
     * @see #leftTo
     */
    protected RedBlackTreeNode left;


    /**
     * The right child node of current node.
     *
     * @see #right()
     * @see #rightTo
     */
    protected RedBlackTreeNode right;

    /* Constructors */

    /**
     * Constructs a new node. The satellite data, color, parent, left child and right child
     * are set to <code>null>/code>.
     *
     * @param _key The key of the node.
     */
    public RedBlackTreeNode(int _key) {
        super(_key);
        color = null;    // black = null, red = "r". Non-key leaves are nulls!!!
        p = null;
        left = null;
        right = null;
    }

    /**
     * Constructs a new node. The parent, color, left child and right child
     * are set to <code>null>/code>.
     *
     * @param _key The key of the node.
     * @param dat  The satellite data of the node.
     */
    public RedBlackTreeNode(int _key, Object dat) {
        super(_key, dat);
        color = null;    // black = null, red = "r". Non-key leaves are nulls!!!
        p = null;
        left = null;
        right = null;
    }

    /**
     * Returns the key of the node.
     *
     * @return The key of the node.
     */
    public int key() {
        return super.key();
    }

    /**
     * Returns the object of the node.
     *
     * @return The object of the node.
     */
    public Object object() {
        return super.object();
    }

    /**
     * Returns the parent of the node.
     *
     * @return The parent of the node.
     */
    public RedBlackTreeNode parent() {
        return this.p;
    }

    /**
     * Returns the right child of the node.
     *
     * @return The right child of the node.
     */
    public RedBlackTreeNode right() {
        return this.right;
    }

    /**
     * Returns the left child of the node.
     *
     * @return The left child of the node.
     */
    public RedBlackTreeNode left() {
        return this.left;
    }

    /**
     * Returns the node as a <code>String</code>.
     *
     * @return The node as a <code>String</code>.
     */
    public String toString() {
        return new String("Key: " + this.key + ", color: " + this.color + ", parent: " + p);
    }


    /**
     * Sets the key of the node to _key.
     *
     * @param _key The new right child node of the node.
     */
    public void keyTo(int _key) {
        super.keyTo(_key);
    }

    /**
     * Sets the satellite data of the node to o.
     *
     * @param o The new satellite data of the node.
     */
    public void objectTo(Object o) {
        super.objectTo(o);
    }

    /**
     * Sets the color of the node to black.
     */
    public void setBlack() {
        this.color = null;
    }

    /**
     * Sets the color of the node to red.
     */
    public void setRed() {
        this.color = "r";
    }

    /**
     * Checks if the color of the node is black.
     *
     * @return True if the color is black.
     */
    public boolean isBlack() {
        if (this.color == null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Checks if the color of the node is red.
     *
     * @return True if the color is red.
     */
    public boolean isRed() {
        if (this.color == "r") {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the parent node to parent.
     *
     * @param parent The new parent node of the node.
     */
    public void parentTo(RedBlackTreeNode parent) {
        this.p = parent;
    }

    /**
     * Sets the right child node to parent.
     *
     * @param r The new right child node of the node.
     */
    public void rightTo(RedBlackTreeNode r)
    {
        this.right = r;
    }

    /**
     * Sets the left child node to parent.
     *
     * @param l The new left child node of the node.
     */
    public void leftTo(RedBlackTreeNode l)
    {
        this.left = l;
    }

} // End class BSTNode
