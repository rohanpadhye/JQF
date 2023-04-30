package edu.berkeley.cs.jqf.fuzz.difffuzz;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/*
    To make outcomes easier to deal with and keep track of
    (diff functions always terminate either with a return value or by throwing something)
 */
public class Outcome implements Serializable {
    public final Object output;
    public final Throwable thrown;

    public Outcome(Object o, Throwable t) {
        output = o;
        thrown = t;
    }

    public static boolean same(Outcome o1, Outcome o2, Method compare) throws InvocationTargetException, IllegalAccessException {
      if ((o1.thrown == null) ^ (o2.thrown == null)) return false;
      if (o1.thrown != null) {
        return o1.thrown.getClass().getName().equals(o2.thrown.getClass().getName());
      }
      return Boolean.TRUE.equals(compare.invoke(null, o1.output, o2.output));
    }

    @Override
    public String toString() {
        if(thrown == null) return "threw nothing, output " + output;
        return "threw " + thrown + ", no output";
    }
}
