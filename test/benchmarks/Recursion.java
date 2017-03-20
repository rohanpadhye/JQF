package benchmarks;

/**
 * @author Rohan Padhye
 */
public class Recursion {
    private static class BinaryTree {
        private static class Node {
            Node left;
            Node right;
            int value;
            Node(int val) { this.value = val; }
        }

        private Node root;

        public void insert(int x) {
            root = insertIntoNode(x, root);
        }

        private Node insertIntoNode(int x, Node n) {
            if (n == null) {
                return new Node(x);
            } else if (x < n.value) {
                n.left = insertIntoNode(x, n.left);
                return n;
            } else if (x > n.value) {
                n.right = insertIntoNode(x, n.right);
                return n;
            } else {
                return n;
            }
        }

        public boolean contains(int x) {
            return nodeContains(root, x);
        }

        private boolean nodeContains(Node n, int x) {
            if (n == null) {
                return false;
            } else if (x < n.value) {
                return nodeContains(n.left, x);
            } else if (x > n.value) {
                return nodeContains(n.right, x);
            } else {
                return true;
            }
        }
    }

    public static void main(String args[]) {
        int[] data = {1, 34, 21, 15, 61, 31, -2, 14, 8, 99, 7};
        BinaryTree tree = new BinaryTree();
        for (int x : data) {
            tree.insert(x);
        }

        int[] queries = {1, 4, 8, 15, 16, 23, 42};
        for (int x: queries) {
            System.out.println(tree.contains(x));
        }
    }
}
