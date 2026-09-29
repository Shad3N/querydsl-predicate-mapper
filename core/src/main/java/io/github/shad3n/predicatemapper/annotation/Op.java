package io.github.shad3n.predicatemapper.annotation;

/**
 * Condition a {@link FilterField} applies between its filter value, read from the DTO field, and its target value,
 * reached through the path.
 * <p>
 * A null filter value adds no condition. A null target value satisfies only {@link #IS_NULL}, and a null object
 * along a nested path satisfies no operator, as with a SQL inner join. Values compare as SQL compares them, with
 * both {@link ToQueryDslPredicateMapper} and {@link ToJavaPredicateMapper}.
 */
public enum Op {
    /**
     * Keeps target values equal to the filter value. The DTO field has the path's type. Numbers are equal by
     * numeric value, so {@code BigDecimal} {@code 1.0} equals {@code 1.00} and {@code 0.0} equals {@code -0.0};
     * offset and zoned date-times are equal when they denote the same instant. Supports
     * {@link FilterField#ignoreCase()} on {@code String} paths.
     */
    EQ,
    /**
     * Keeps target values different from the filter value, under the equality of {@link #EQ}. The DTO field has
     * the path's type. Supports {@link FilterField#ignoreCase()} on {@code String} paths.
     */
    NOT_EQ,
    /**
     * Keeps target values less than or equal to the filter value; offset and zoned date-times are ordered by
     * instant. The path is {@code Comparable} and the DTO field has its type.
     */
    LTE,
    /**
     * Keeps target values greater than or equal to the filter value; offset and zoned date-times are ordered by
     * instant. The path is {@code Comparable} and the DTO field has its type.
     */
    GTE,
    /**
     * Keeps target values matching the filter's SQL LIKE pattern as a whole: {@code %} matches any run of
     * characters, {@code _} any single character, and {@code !} makes the character after it match itself, so
     * {@code !%}, {@code !_} and {@code !!} match a literal {@code %}, {@code _} and {@code !}. The path and the
     * DTO field are {@code String}. Supports {@link FilterField#ignoreCase()}.
     */
    LIKE,
    /**
     * Keeps target values containing the filter value as a literal substring; no character acts as a wildcard,
     * and an empty filter value keeps every non-null target value. The path and the DTO field are
     * {@code String}. Supports {@link FilterField#ignoreCase()}.
     */
    CONTAINS,
    /**
     * Keeps target values equal to an element of the filter collection, under the equality of {@link #EQ}; null
     * elements match nothing, and an empty collection keeps nothing. The DTO field is a {@code Collection} whose
     * element type is the path's type.
     */
    IN,
    /**
     * Keeps null target values when the {@code Boolean} DTO field is {@code true}; {@code false} adds no
     * condition. With {@link ToJavaPredicateMapper}, a path ending in a primitive fails compilation.
     */
    IS_NULL,
    /**
     * Keeps non-null target values when the {@code Boolean} DTO field is {@code true}; {@code false} adds no
     * condition. With {@link ToJavaPredicateMapper}, a path ending in a primitive fails compilation.
     */
    IS_NOT_NULL,
    /**
     * Keeps target values in which the filter's regular expression is found anywhere. The path and the DTO field
     * are {@code String}. Supports {@link FilterField#ignoreCase()}. Supported by {@link ToJavaPredicateMapper}
     * only; a {@link ToQueryDslPredicateMapper} method mapping it fails compilation.
     * <p>
     * Building the predicate throws {@link IllegalArgumentException} for an invalid expression, and a match running
     * past its time limit throws {@link IllegalStateException}, so a pattern that backtracks without bound fails
     * instead of blocking its thread.
     */
    REGEX
}
