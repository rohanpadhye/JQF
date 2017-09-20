/*
 The MIT License

 Copyright (c) 2017 University of California, Berkeley

 Permission is hereby granted, free of charge, to any person obtaining
 a copy of this software and associated documentation files (the
 "Software"), to deal in the Software without restriction, including
 without limitation the rights to use, copy, modify, merge, publish,
 distribute, sublicense, and/or sell copies of the Software, and to
 permit persons to whom the Software is furnished to do so, subject to
 the following conditions:

 The above copyright notice and this permission notice shall be
 included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/
package jwig.fuzz.junit;

import jwig.fuzz.guidance.Guidance;
import jwig.fuzz.guidance.GuidanceIOException;
import jwig.fuzz.guidance.NoGuidance;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;

public class GuidedFuzzing {

    private static Guidance guidance;

    private static long DEFAULT_MAX_TRIALS = 100;

    public static void setGuidance(Guidance g) {
        if (guidance != null) {
            throw new IllegalStateException("Can only set guided once.");
        }
        guidance = g;
    }

    public static Guidance getGuidance() {
        if (guidance == null) {
            System.err.println(String.format("Warning: No guidance set. " +
                    " Falling back to default %d trials with no feedback", DEFAULT_MAX_TRIALS));
            setGuidance(new NoGuidance(100_000L));
        }

        return guidance;
    }

    private static void unsetGuidance() {
        guidance = null;
    }

    public synchronized static void run(Class<?> testClass,
                           String testMethod, Guidance guidance) {


        // Set the static guided instance
        setGuidance(guidance);

        // Create a JUnit Request
        Request testRequest = Request.method(testClass, testMethod);

        // Run the test until it ends or throws
        try {
            JUnitCore junit = new JUnitCore();
            junit.addListener(new TextListener(System.out));
            junit.run(testRequest);
            System.out.println("Guided Quickcheck completed successfully.");
        } catch (GuidanceIOException e) {
            System.err.println("Guided Quickcheck terminated.");
            e.getCause().printStackTrace();
        } finally {
            unsetGuidance();
        }

    }

}
