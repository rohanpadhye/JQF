package edu.berkeley.cs.jqf.fuzz.difffuzz;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@link Comparison} annotation marks a method as a comparison
 * function for regression-based fuzz testing.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Comparison {
}
