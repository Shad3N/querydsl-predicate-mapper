package io.github.shad3n.predicatemapper;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.Optional;

/**
 * Finds the type argument a type supplies to a generic supertype with one type parameter.
 */
final class SupertypeArguments {

    private SupertypeArguments() {
    }

    /**
     * Finds the type argument the type supplies to the generic type, searching the type's supertypes; the upper
     * bound stands for a wildcard argument.
     *
     * @param types         the type utilities of the processing environment
     * @param type          the type to search from
     * @param erasedGeneric the erasure of the generic type
     * @return the argument, or empty when the type does not extend the generic type, extends it raw, or supplies an
     * unbounded wildcard
     */
    static Optional<TypeMirror> of(Types types, TypeMirror type, TypeMirror erasedGeneric) {
        if (type.getKind() != TypeKind.DECLARED) {
            return Optional.empty();
        }
        if (types.isSameType(types.erasure(type), erasedGeneric)) {
            List<? extends TypeMirror> arguments = ((DeclaredType) type).getTypeArguments();
            if (arguments.size() != 1) {
                return Optional.empty();
            }
            TypeMirror argument = arguments.get(0);
            return argument.getKind() == TypeKind.WILDCARD
                    ? Optional.ofNullable(((WildcardType) argument).getExtendsBound())
                    : Optional.of(argument);
        }
        return types.directSupertypes(type).stream()
                    .map(supertype -> of(types, supertype, erasedGeneric))
                    .flatMap(Optional::stream)
                    .findFirst();
    }
}
