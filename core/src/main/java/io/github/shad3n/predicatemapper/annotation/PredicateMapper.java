package io.github.shad3n.predicatemapper.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface for APT-generated predicate mapper implementation.
 * Each method on the interface should be annotated with {@link ToPredicate}.
 * The APT generates a Spring {@code @Component} implementing the interface,
 * keeping Q-class references private to the service module.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface PredicateMapper {
}
