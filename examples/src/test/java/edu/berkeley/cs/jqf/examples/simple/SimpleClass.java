package edu.berkeley.cs.jqf.examples.simple;

public class SimpleClass {
    public static int test(int a) {
        int b = 0;
        if (a > 0) {
            b += 1;
        } else {
            b -= 1;
        }
        if (a % 2 == 0) {
            b += 1;
        } else {
            b -= 1;
        }
        return b;
    }
}
