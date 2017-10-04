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
 * A <code>Node</code> object is a node of search tree
 * including key data and satellite object.
 * <p/>
 * It can be used with binary search tree as well as with
 * red black tree or with any other search tree.
 *
 * @author Tuomo Saarni
 * @version 1.2, 08/16/01
 */

public class Node {
    /**
     * The key of the node.
     *
     * @see #key()
     * @see #keyTo
     */
    protected int key;

    /**
     * The satellite data in the node.
     *
     * @see #object()
     * @see #objectTo
     */
    protected Object data;                // Refers to the satellite data

    /**
     * Constructs a new node. The satellite data is set to <code>null>/code>.
     *
     * @param _key The key of the node.
     */
    public Node(int _key) {
        key = _key;
        data = null;
    }

    /**
     * Constructs a new node.
     *
     * @param _key The key of the node.
     * @param dat  The satellite data of the node, type <code>Object</code>.
     */
    public Node(int _key, Object dat) {
        this(_key);
        Object data = dat;
    }

    /**
     * Returns the key of the node.
     *
     * @return The key of the node.
     */
    public int key() {
        return this.key;
    }

    /**
     * Returns the satellite data of the node.
     *
     * @return The satellite object of the node.
     */
    public Object object() {
        return this.data;
    }

    /**
     * Returns the node.
     *
     * @return The node as a <code>String</code>.
     */
    public String toString() {
        return new String("Key: " + this.key);
    }

    /**
     * Sets the key to _key.
     *
     * @param _key The new key of the node.
     */
    public void keyTo(int _key) {
        this.key = _key;
    }

    /**
     * Sets the data to o.
     *
     * @param o The new data of the node.
     */
	public void objectTo(Object o)
	{
		this.data = o;
	}

} // End class Node
