package edu.berkeley.cs.jqf.fuzz.difffuzz;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@link DiffFuzz} annotation marks a method as an entry-point for
 * regression-based fuzz testing.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DiffFuzz {
    String cmp() default "";
}
