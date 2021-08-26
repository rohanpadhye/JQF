package edu.berkeley.cs.jqf.examples;

import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.runner.RunWith;

// Testing fix for: https://github.com/rohanpadhye/JQF/issues/65
@RunWith(JQF.class)
public class ConstructorExceptionTest {

    class Foo {
        public Foo(int x) throws Exception {
            this(thr0w());
        }

        public Foo(boolean b) {
            // Do nothing
        }
    }

    static boolean thr0w() throws Exception {
        throw new Exception();
    }

    @Fuzz
    public void testThrowingConstructors(int x)  throws Exception {
        new Foo(x);
    }
}
