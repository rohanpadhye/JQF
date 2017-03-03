package benchmarks;

import jwig.logging.SingleSnoop;

/**
 * @author Rohan Padhye
 */
public class StaticInitDriver {
    static {
        SingleSnoop.startSnooping();
    }

    public static void main(String[] args) {
        System.out.println(StaticInit.get());
    }
}