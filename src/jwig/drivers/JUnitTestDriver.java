/*
 * Copyright (c) 2017, University of California, Berkeley
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jwig.drivers;

import java.io.File;
import java.io.IOException;

import com.pholser.junit.quickcheck.guided.Guidance;
import com.pholser.junit.quickcheck.guided.GuidanceManager;
import com.pholser.junit.quickcheck.guided.GuidedJunitQuickcheckTest;
import jwig.logging.SingleSnoop;

/**
 * @author Rohan Padhye
 */
public class JUnitTestDriver {

    private static Guidance guidance = new Guidance() {
        @Override
        public File inputFile() {
            return new File("/dev/urandom");
        }

        @Override
        public void waitForInput() throws IOException {

        }

        @Override
        public void notifyEndOfRun(boolean success) throws IOException {

        }
    };

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java " + JUnitTestDriver.class + " TEST_CLASS TEST_METHOD [TEST_INPUT_FILE]");
            System.exit(1);
        }

        String testClassName  = args[0];
        String testMethodName = args[1];
        String testInputFile  = args.length > 2 ? args[2] : null;

        try {
            // Load test class
            Class<? extends GuidedJunitQuickcheckTest> testClass =
                    Class.forName(testClassName, true, ClassLoader.getSystemClassLoader())
                    .asSubclass(GuidedJunitQuickcheckTest.class);

            // Start tracing
            SingleSnoop.startSnooping();

            // Run the Junit test
            GuidanceManager.run(testClass, testMethodName, guidance);


        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            System.err.println(String.format("Cannot load class %s", testClassName));
            e.printStackTrace();
            System.exit(2);
        } catch (ClassCastException e) {
            System.err.println(String.format("%s is not a junit-quickcheck-guided test class", testClassName));
            System.exit(3);
        } /* catch (NoSuchMethodException e) {
            System.err.println(String.format("%s is not a method of the test class", testMethodName));
            System.exit(4);
        } */

    }
}
