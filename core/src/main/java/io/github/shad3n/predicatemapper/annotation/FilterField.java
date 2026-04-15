package io.github.shad3n.predicatemapper.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.CLASS)
public @interface FilterField {
    /**
     * Dot-separated path on the Q-class
     * <p>
     * Example:
     * {@code "price"} or {@code "category.name"}.
     */
    String path();

    Op op();
}
