package io.github.shad3n.predicatemapper.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares which Q-class to use when generating the predicate for a given
 * {@link PredicateMapper} method. The Q-class must be on the compilation
 * classpath of the service module — it never leaks into the shared library.
 *
 * <p>The annotated method must have exactly one parameter: the {@code FilterDto}
 * whose fields carry {@link FilterField} annotations.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface ToPredicate {
    /**
     * The QueryDSL Q-class to build predicates against.
     */
    Class<?> value();
}
