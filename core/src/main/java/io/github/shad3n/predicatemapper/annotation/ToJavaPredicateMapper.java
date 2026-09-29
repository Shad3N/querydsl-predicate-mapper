package io.github.shad3n.predicatemapper.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates a {@link PredicateMapper} method returning a {@code java.util.function.Predicate} that tests
 * plain Java objects of the given class, reached through record accessors or getters. The generated code
 * references only JDK types, so no QueryDSL is needed on the classpath.
 *
 * <p>The annotated method takes exactly one parameter, the filter DTO whose fields carry
 * {@link FilterField} annotations, and returns {@code java.util.function.Predicate<T>} where {@code T} is
 * the class given here. A null DTO yields a predicate accepting every object.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface ToJavaPredicateMapper {
    /**
     * The class whose instances the predicate tests.
     */
    Class<?> value();
}
