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
 * A <code>RedBlackTree</code> object is a Red-Black
 * tree.
 * <p/>
 *
 * @author Tuomo Saarni
 * @version 1.1, 08/16/01
 */
public class RedBlackTree {
    /**
     * The root node of current tree.
     */
    private RedBlackTreeNode root;

    /**
     * Constructs a new empty tree.
     */
    public RedBlackTree() {
        root = null;
    }

    /**
     * Constructs a new tree with a root node x.
     *
     * @param x The root node of the tree.
     */
    public RedBlackTree(RedBlackTreeNode x) {
        root = x;
    }

    /**
     * Inserts a node to the tree.
     * Runs in O(lg n) time.
     *
     * @param x The new node of the tree.
     * @throws NullPointerException If the parameter x is null.
     */
    public void treeInsert(RedBlackTreeNode x) throws NullPointerException {
        RedBlackTreeNode y = null;
        RedBlackTreeNode z = this.root;

        while (z != null) {
            y = z;
            if (x.key() < z.key()) {
                z = z.left();
            } else {
                z = z.right();
            }
        }
        x.parentTo(y);
        if (y == null) {
            this.root = x;
        } else {
            if (x.key() < y.key()) {
                y.leftTo(x);
            } else {
                y.rightTo(x);
            }
        }


        x.setRed();

        while (x != this.root && (x.parent()).isRed()) {

            if (x.parent() == ((x.parent()).parent()).left()) {
                y = ((x.parent()).parent()).right();

                if (y != null && y.isRed()) {
                    (x.parent()).setBlack();
                    y.setBlack();
                    ((x.parent()).parent()).setRed();
                    x = (x.parent()).parent();
                } else {
                    if (x == (x.parent()).right()) {
                        x = x.parent();
                        leftRotate(x);
                    }

                    (x.parent()).setBlack();
                    ((x.parent()).parent()).setRed();
                    rightRotate((x.parent()).parent());
                }
            } else {
                y = ((x.parent()).parent()).left();

                if (y != null && y.isRed()) {
                    (x.parent()).setBlack();
                    y.setBlack();
                    ((x.parent()).parent()).setRed();
                    x = (x.parent()).parent();
                } else {
                    if (x == (x.parent()).left()) {
                        x = x.parent();
                        rightRotate(x);
                    }

                    (x.parent()).setBlack();
                    ((x.parent()).parent()).setRed();
                    leftRotate((x.parent()).parent());
                }
            }
        } // End while

        (this.root).setBlack();
    }

    /**
     * Rotates the tree from the node x to left.
     * Runs in Theta(n) time.
     *
     * @param    x    The node from which to start.
     */
    private void leftRotate(RedBlackTreeNode x) {
        RedBlackTreeNode y = x.right();
        x.rightTo(y.left());
        if (y.left() != null) {
            (y.left()).parentTo(x);
        }
        y.parentTo(x.parent());

        if (x.parent() == null) {
            this.root = y;
        } else {
            if (x == (x.parent()).left()) {
                (x.parent()).leftTo(y);
            } else {
                (x.parent()).rightTo(y);
            }
        }

        y.leftTo(x);
        x.parentTo(y);
    }

    /**
     * Rotates the tree from the node x to right.
     * Runs in Theta(n) time.
     *
     * @param    x    The node from which to start.
     */
    private void rightRotate(RedBlackTreeNode x) {
        RedBlackTreeNode y = x.left();
        x.leftTo(y.right());
        if (y.right() != null) {
            (y.right()).parentTo(x);
        }
        y.parentTo(x.parent());

        if (x.parent() == null) {
            this.root = y;
        } else {
            if (x == (x.parent()).right()) {
                (x.parent()).rightTo(y);
            } else {
                (x.parent()).leftTo(y);
            }
        }

        y.rightTo(x);
        x.parentTo(y);
    }

    /**
     * Deletes the given node from the tree.
     * Runs in O(lg n) time.
     *
     * @param z The node to be deleted of the tree.
     * @throws NullPointerException If the parameter z is null.
     */
    public RedBlackTreeNode treeDelete(RedBlackTreeNode z) throws NullPointerException {
        RedBlackTreeNode x = new RedBlackTreeNode(0);
        RedBlackTreeNode y = new RedBlackTreeNode(0);
        RedBlackTreeNode sentinel = new RedBlackTreeNode(0);

        if (z.left() == null || z.right() == null) {
            y = z;
        } else {
            y = treeSuccessor(z);
        }
        // Now y != null, y = z or greater

        if (y.left() != null) {
            x = y.left();
        } else {
            if (y.right() != null) {
                x = y.right();
            } else {
                x = sentinel;
            }
        }

        x.parentTo(y.parent());

        if (y.parent() == null) // if y is root
        {
            this.root = x;
        } else // y is not root
        {
            if (y == (y.parent()).left()) // if y = left[p[y]]
            {
                (y.parent()).leftTo(x); // then left[p[y]] <- x
            } else {
                (y.parent()).rightTo(x);
            }
        }
        if (y != z) {
            z.keyTo(y.key());
            z.objectTo(y.object());
        }
        if (y.isBlack()) {
            deleteFixup(x);
        }

        // Remove all references to sentinel
        // Otherwise sentinel can be 'seen' in the tree
        if (this.root != sentinel) {
            if ((sentinel.parent()).left() == sentinel) {
                (sentinel.parent()).leftTo(null);
            }
            if ((sentinel.parent()).right() == sentinel) {
                (sentinel.parent()).rightTo(null);
            }
            sentinel = null;
        } else {
            this.root = null;
            sentinel = null;
        }

        return y;
    }

    /**
     * Fixes the red-black tree after a deletion if needed.
     *
     * @param x The node from which to start.
     */
    private void deleteFixup(RedBlackTreeNode x) {
        RedBlackTreeNode w = new RedBlackTreeNode(0);

        while (x != this.root && x.isBlack()) {
            if (x == (x.parent()).left()) //x is left son of its parent
            {
                w = (x.parent()).right(); // set w to refer x's parent's right son
                if (w.isRed()) {
                    w.setBlack();
                    (x.parent()).setRed();
                    leftRotate(x.parent());
                    w = (x.parent()).right();
                }
                if ((w.left() == null && w.right() == null) ||
                        (w.left() == null && w.right() != null && (w.right()).isBlack()) ||
                        (w.right() == null && w.left() != null && (w.left()).isBlack()) ||
                        (w.right() != null && w.left() != null
                                && (w.right()).isBlack() && (w.left()).isBlack())
                        ) {
                    w.setRed();
                    x = x.parent();
                } else {
                    if (w.right() != null && (w.right()).isBlack()) {
                        (w.left()).setBlack();
                        w.setRed();
                        rightRotate(w);
                        w = (x.parent()).right();
                    }
                    if ((x.parent()).isBlack()) {
                        w.setBlack();
                    } else {
                        w.setRed();
                    }
                    (x.parent()).setBlack();
                    if (w.right != null) // Otherwise its already black
                    {
                        (w.right()).setBlack();
                    }
                    leftRotate(x.parent());
                    x = this.root;
                }
            } else {
                w = (x.parent()).left(); // set w to refer x's parent's right son
                if (w.isRed()) {
                    w.setBlack();
                    (x.parent()).setRed();
                    rightRotate(x.parent());
                    w = (x.parent()).left();
                }
                if ((w.left() == null && w.right() == null) ||
                        (w.left() == null && w.right() != null && (w.right()).isBlack()) ||
                        (w.right() == null && w.left() != null && (w.left()).isBlack()) ||
                        (w.right() != null && w.left() != null
                                && (w.right()).isBlack() && (w.left()).isBlack())
                        ) {
                    w.setRed();
                    x = x.parent();
                } else {
                    if (w.left() != null && (w.left()).isBlack()) {
                        (w.right()).setBlack();
                        w.setRed();
                        leftRotate(w);
                        w = (x.parent()).left();
                    }
                    if ((x.parent()).isBlack()) {
                        w.setBlack();
                    } else {
                        w.setRed();
                    }
                    (x.parent()).setBlack();
                    if (w.left != null) // Otherwise its already black
                    {
                        (w.left()).setBlack();
                    }
                    rightRotate(x.parent());
                    x = this.root;
                }
            }
        } // End while
        x.setBlack();
    } // End deleteFixup

    /**
     * Prints the keys of current tree in inorder (ascending).
     * Runs in Theta(n) time.
     *
     * @param    x    The node from which to start.
     */
    public void inorderTreeWalk(RedBlackTreeNode x, String space) {
        if (!(x == null)) {
            System.out.println(space + x.key());
            inorderTreeWalk(x.left(), space + "    ");
            inorderTreeWalk(x.right(), space + "    ");
        }

    }

    /**
     * Searches a node with key k starting from the node x which is usually the root.
     * If the node is not found returns <code>null</code> otherwise returns the pointer
     * to the current node.Runs in O(h) time where h is the height of the tree. Works recursively.
     *
     * @return The node with key k or <code>null</code>.
     * @param    x    The node from which to start, usually the root.
     * @param    k    The key of the wanted node.
     */
    public RedBlackTreeNode treeSearch(RedBlackTreeNode x, int k) {
        if (x == null || k == x.key()) {
            return x;
        }
        if (k < x.key()) {
            return treeSearch(x.left(), k);
        } else {
            return treeSearch(x.right(), k);
        }
    }

    /**
     * Searches a node with key k starting from the node x which is usually the root.
     * If the node is not found returns <code>null</code> otherwise returns the pointer
     * to the current node. Runs in O(h) time where h is the height of the tree. Works iteratively.
     *
     * @return The node with key k or <code>null</code>.
     * @param    x    The node from which to start, usually the root.
     * @param    k    The key of the wanted node.
     */
    public RedBlackTreeNode iterativeTreeSearch(RedBlackTreeNode x, int k) {
        while (!(x == null) && k != x.key()) {
            if (k < x.key()) {
                x = x.left();
            } else {
                x = x.right();
            }
        }
        return x;
    }

    /**
     * Searches a node with smallest key starting from the node x which is usually the root.
     * Runs in O(h) time where h is the height of the tree.
     *
     * @return The node with the smallest key.
     * @throws NullPointerException If the parameter x is null.
     * @param    x    The node from which to start, usually the root.
     */
    public RedBlackTreeNode treeMinimum(RedBlackTreeNode x) throws NullPointerException {
        while (x.left() != null) {
            x = x.left();
        }
        return x;
    }

    /**
     * Searches a node with biggest key starting from the node x which is usually the root.
     * Runs in O(h) time where h is the height of the tree.
     *
     * @return The node with the biggest key.
     * @throws NullPointerException If the parameter x is null.
     * @param    x    The node from which to start, usually the root.
     */
    public RedBlackTreeNode treeMaximum(RedBlackTreeNode x) throws NullPointerException {
        while (x.right() != null) {
            x = x.right();
        }
        return x;
    }

    /**
     * Searches the successor node of the key x.
     * Runs in O(h) time where h is the height of the tree.
     *
     * @return The successor node.
     * @throws NullPointerException If the parameter x is null.
     * @param    x    The node from which successor is wanted.
     */
    public RedBlackTreeNode treeSuccessor(RedBlackTreeNode x) throws NullPointerException {
        if (x.right() != null) {
            return treeMinimum(x.right());
        }
        RedBlackTreeNode y = x.parent();
        while (y != null && x.equals(y.right())) {
            x = y;
            y = y.parent();
        }
        return y;
    }

    /**
     * Searches the predessor node of the key x.
     * Runs in O(h) time where h is the height of the tree.
     *
     * @return The predessor node.
     * @throws NullPointerException If the parameter x is null.
     * @param    x    The node from which predessor is wanted.
     */
    public RedBlackTreeNode treePredessor(RedBlackTreeNode x) throws NullPointerException {
        if (x.left() != null) {
            return treeMaximum(x.left());
        }
        RedBlackTreeNode y = x.parent();
		while (y != null && x.equals(y.left()))
		{
			x = y;
			y = y.parent();
		}
		return y;
	}

	/**
	 * Returns the root of the tree.
	 *
	 * @return	The root of the tree.
	 */
	public RedBlackTreeNode root()
	{
		return this.root;
	}


} // End class RedBlackTree
