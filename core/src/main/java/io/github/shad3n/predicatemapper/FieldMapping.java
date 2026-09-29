package io.github.shad3n.predicatemapper;


import io.github.shad3n.predicatemapper.annotation.Op;

/**
 * Represents the mapping between a DTO field and the target path it filters on.
 *
 * @param dtoFieldName the name of the field in the DTO
 * @param op           the operation to apply (e.g., EQ, IN, LIKE)
 * @param ignoreCase   whether text is compared ignoring case
 * @param getterName   the name of the no-argument DTO method reading the field (e.g., "getName" or "name")
 * @param target       the path resolved against the mapper method's target
 */
record FieldMapping(String dtoFieldName, Op op, boolean ignoreCase, String getterName, ResolvedPath target) {
}
