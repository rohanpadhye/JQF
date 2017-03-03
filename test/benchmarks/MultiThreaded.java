package benchmarks;

import jwig.logging.SingleSnoop;

/**
 * @author Rohan Padhye
 */
public class MultiThreaded {
    static  {
        SingleSnoop.startSnooping();
    }
    public static void main(String[] args) throws InterruptedException {
        NamePrinter n1 = new NamePrinter("Numba won");
        NamePrinter n2 = new NamePrinter("Numba too");
        n1.start();
        n2.start();
        n1.join();
        n2.join();
    }

    static class NamePrinter extends Thread {
        NamePrinter(String name) {
            super(name);
        }
        @Override
        public void run() {
            System.out.println("I am thread: " + Thread.currentThread().getName());
        }
    };
}
