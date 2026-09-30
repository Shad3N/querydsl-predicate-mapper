package io.github.shad3n.predicatemapper;

import com.palantir.javapoet.ClassName;

import java.util.List;

/**
 * Represents the mapping configuration for a single method in a {@code @PredicateMapper} interface.
 *
 * @param methodName the name of the method to generate
 * @param backend    the backend generating the method
 * @param target     the JavaPoet ClassName of the Q-class or target class
 * @param dtoClass   the JavaPoet ClassName of the DTO parameter
 * @param fields     the list of field mappings associated with this method
 */
record MethodMapping(
        String methodName,
        PredicateBackend backend,
        ClassName target,
        ClassName dtoClass,
        List<FieldMapping> fields) {
}
