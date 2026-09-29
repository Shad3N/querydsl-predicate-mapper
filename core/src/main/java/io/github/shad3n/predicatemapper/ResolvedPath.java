package io.github.shad3n.predicatemapper;

import javax.lang.model.type.TypeMirror;
import java.util.List;

/**
 * A {@code @FilterField} path resolved against a mapper method's target.
 *
 * @param members member names followed from the target, one per path segment: Q-class fields for
 *                QueryDSL mappers, accessor methods for Java predicate mappers
 * @param endType type of the last member
 */
record ResolvedPath(List<String> members, TypeMirror endType) {
}
