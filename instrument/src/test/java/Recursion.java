package benchmarks;

/**
 * @author Rohan Padhye
 */
public class Recursion {

    public static void main(String args[]) {
        int[] data = {1, 34, 21, 15, 61, 31, -2, 14, 8, 99, 7};
        benchmarks.BinaryTree tree = new benchmarks.BinaryTree();
        for (int x : data) {
            tree.insert(x);
        }

        int[] queries = {1, 4, 8, 15, 16, 23, 42};
        for (int x: queries) {
            System.out.println(tree.contains(x));
        }
    }
}
