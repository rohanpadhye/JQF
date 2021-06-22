package edu.berkeley.cs.jqf.instrument.util;

import java.util.function.Function;

/**
 * ThrowingFunction - A function which throws an exception
 */
@FunctionalInterface
public interface ThrowingFunction<A, B, E extends Exception> {
    static <X, Y, E extends Exception> Function<X, Y> wrap(ThrowingFunction<X, Y, E> f) {
        return x -> {
            try { return f.run(x); }
            catch (Exception e) { throw new RuntimeException(e); }
        };
    }

    abstract B run(A a) throws E;
}
