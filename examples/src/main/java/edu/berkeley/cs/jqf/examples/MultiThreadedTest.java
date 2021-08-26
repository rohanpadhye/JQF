package edu.berkeley.cs.jqf.examples;

import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.runner.RunWith;

@RunWith(JQF.class)
public class MultiThreadedTest {

    private volatile int counter = 0;

    @Fuzz
    public void testMultiThreaded(int x) {
        Thread t1 = new Thread(() -> {
            if (x > 10) {
                if (x < 20) {
                    counter += x;
                }
            }
        });
        Thread t2 = new Thread(() -> {
            if (x < 25) {
                if (x > 15) {
                    counter += x;
                }
            }
        });
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }


}
