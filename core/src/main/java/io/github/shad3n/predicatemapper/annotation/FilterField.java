package io.github.shad3n.predicatemapper.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Maps a filter DTO field to a condition on the mapper method's target: the field's value, when set, is
 * compared through {@link #op()} with the value {@link #path()} reaches.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface FilterField {
    /**
     * Dot-separated path on the mapper's target, such as {@code "price"} or {@code "category.name"}: Q-class
     * fields for QueryDSL mappers, record accessors or getters for Java predicate mappers.
     */
    String path();

    /**
     * Condition between the field's value and the value the path reaches.
     */
    Op op();

    /**
     * Compares text ignoring case, folding case one character at a time across Unicode. Applies to
     * {@link Op#EQ}, {@link Op#NOT_EQ}, {@link Op#LIKE}, {@link Op#CONTAINS} and {@link Op#REGEX} on
     * {@code String} paths; any other use fails compilation.
     */
    boolean ignoreCase() default false;

    /**
     * Name of the no-argument DTO method generated code reads the field's value through, such as
     * {@code "fetchPrice"} for {@code dto.fetchPrice()}. When empty, the record accessor on records, and
     * {@code getField()} then {@code isField()} on other classes.
     */
    String getter() default "";
}
