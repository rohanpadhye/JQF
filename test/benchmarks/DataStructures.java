package benchmarks;

import java.util.*;

/**
 * @author Rohan Padhye
 */
public class DataStructures {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        Map<String, String> env = System.getenv();
        for (int i = 0; i < 1; i++) {
            NavigableMap<String, String> treeMap = new TreeMap<>(env);
            for (Map.Entry<String, String> e : treeMap.entrySet()) {
                System.out.println(e.getKey() + " = " + e.getValue());
            }
            List<String> values = new LinkedList<>(treeMap.values());
            Collections.sort(values);
            for (String val : values) {
                System.out.println("value --> " + val);
            }
        }
        long endTime = System.currentTimeMillis();
        System.err.println("Total time = " + (endTime - startTime) + " ms");
    }
}
