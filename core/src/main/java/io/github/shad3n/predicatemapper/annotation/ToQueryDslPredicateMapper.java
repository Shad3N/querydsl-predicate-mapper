package io.github.shad3n.predicatemapper.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generates a {@link PredicateMapper} method returning a QueryDSL {@code com.querydsl.core.types.Predicate}
 * built against the given Q-class. The Q-class must be on the compilation classpath of the service module;
 * it never leaks into the shared library.
 *
 * <p>The annotated method takes exactly one parameter: the filter DTO whose fields carry
 * {@link FilterField} annotations. {@link Op#REGEX} fields are rejected, since QueryDSL's JPA templates
 * rewrite regular expressions into {@code LIKE}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface ToQueryDslPredicateMapper {
    /**
     * The QueryDSL Q-class to build predicates against.
     */
    Class<?> value();
}
