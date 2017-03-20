package benchmarks;

import java.util.TreeSet;

/**
 * @author Rohan Padhye
 */
public class MultiThreaded {
    public static void main(String[] args) throws InterruptedException {
        NamePrinter n0 = new NamePrinter();
        NamePrinter n1 = new NamePrinter("Numba1");
        NamePrinter n2 = new NamePrinter("Numba2");
        n0.start();
        n1.start();
        n2.start();
        n1.join();
        n2.join();
        n0.join();
    }

    static class NamePrinter extends Thread {
        NamePrinter() {
            super();
        }
        NamePrinter(String name) {
            super(name);
        }
        @Override
        public void run() {
            TreeSet<Character> map = new TreeSet<>();
            for (char c : this.getName().toCharArray()) {
                map.add(c);
            }

            System.out.println("I am thread: " + Thread.currentThread().getName());
            System.out.println(map.contains('u'));
        }
    };
}
