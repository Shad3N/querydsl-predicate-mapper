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

    /**
     * Overrides the getter method name used in generated code.
     * <p>
     * If set, the processor uses {@code dto.<getter>()} directly instead of inferring
     * from the field name. Useful for non-standard accessor names.
     * <p>
     * Example: {@code getter = "fetchPrice"} → {@code dto.fetchPrice()}
     */
    String getter() default "";
}
