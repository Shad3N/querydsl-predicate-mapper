package io.github.shad3n.predicatemapper;

import com.palantir.javapoet.ClassName;

import java.util.List;

/**
 * Represents the mapping configuration for a single method in a {@code @io.github.shad3n.annotation.PredicateMapper} interface.
 *
 * @param methodName the name of the method to generate
 * @param qClass     the JavaPoet ClassName of the QueryDSL Q-class
 * @param dtoClass   the JavaPoet ClassName of the DTO parameter
 * @param fields     the list of field mappings associated with this method
 */
record MethodMapping(
        String methodName,
        ClassName qClass,
        ClassName dtoClass,
        List<FieldMapping> fields) {
}
