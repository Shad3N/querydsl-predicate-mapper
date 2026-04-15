package io.github.shad3n.predicatemapper;


import io.github.shad3n.predicatemapper.annotation.Op;

/**
 * Represents the mapping between a DTO field and its corresponding QueryDSL path and operation.
 *
 * @param dtoFieldName the name of the field in the DTO
 * @param path         the dot-separated path to the field in the QueryDSL Q-class
 * @param op           the operation to apply (e.g., EQ, IN, LIKE)
 */
record FieldMapping(String dtoFieldName, String path, Op op) {
}
