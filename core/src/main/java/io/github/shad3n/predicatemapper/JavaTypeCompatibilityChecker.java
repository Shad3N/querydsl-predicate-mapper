package io.github.shad3n.predicatemapper;

import com.google.auto.common.MoreTypes;
import io.github.shad3n.predicatemapper.annotation.Op;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.time.OffsetDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Validates DTO field types against the accessor types a path resolves to on a plain Java target, and
 * classifies path types for the comparisons generated against them. Primitive and boxed forms of the same type
 * are treated as equal.
 */
class JavaTypeCompatibilityChecker {

    private final ProcessingEnvironment processingEnv;
    private final Types types;
    private final TypeMirror comparableType;
    private final TypeMirror collectionType;
    private final List<TypeMirror> instantTypes;
    private final List<TypeMirror> floatingPointTypes;

    public JavaTypeCompatibilityChecker(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.types = processingEnv.getTypeUtils();
        this.comparableType = erasedType(Comparable.class);
        this.collectionType = erasedType(Collection.class);
        this.instantTypes = List.of(erasedType(OffsetDateTime.class), erasedType(ChronoZonedDateTime.class));
        this.floatingPointTypes = List.of(erasedType(Double.class), erasedType(Float.class));
    }

    /**
     * Checks that the DTO field fits the resolved path under the given operator: a Boolean for null checks,
     * a collection whose element type is the path type for IN, a String on both sides for LIKE, CONTAINS and
     * REGEX, a String path for ignoreCase, a Comparable path for LTE and GTE, and the path type itself otherwise.
     *
     * @param target     the resolved path
     * @param path       the declared path for error messaging
     * @param dtoField   the DTO field element
     * @param dtoType    the DTO type for error anchoring
     * @param op         the operator to use for the field
     * @param ignoreCase whether text is compared ignoring case
     * @return true if compatible, false after reporting an error otherwise
     */
    public boolean check(ResolvedPath target, String path, VariableElement dtoField, TypeElement dtoType, Op op,
                         boolean ignoreCase) {
        String fieldName = dtoField.getSimpleName().toString();
        TypeMirror endType = target.endType();
        TypeMirror pathType = boxed(endType);
        TypeMirror fieldType = dtoField.asType();

        if (ignoreCase && !isString(pathType)) {
            return error(ProcessorErrorMessageFactory.buildIgnoreCaseTypeMessage(path, fieldName, endType.toString()),
                         dtoType);
        }

        return switch (op) {
            case IS_NULL, IS_NOT_NULL -> {
                if (endType.getKind().isPrimitive()) {
                    yield error(ProcessorErrorMessageFactory.buildNullCheckOnPrimitiveMessage(
                            path, fieldName, op.name(), endType.toString()), dtoType);
                }
                yield expect(isBoolean(boxed(fieldType)), path, fieldName, "java.lang.Boolean", fieldType, dtoType);
            }
            case IN -> expect(collectionElementType(fieldType).filter(element -> isSameErasure(element, pathType))
                                                             .isPresent(),
                              path, fieldName, "java.util.Collection<" + pathType + ">", fieldType, dtoType);
            case LIKE, CONTAINS, REGEX -> expect(isString(pathType) && isString(fieldType), path, fieldName,
                                                 "java.lang.String", fieldType, dtoType);
            case LTE, GTE -> expect(isAssignableToAny(pathType, List.of(comparableType))
                                            && isSameErasure(boxed(fieldType), pathType), path,
                                    fieldName, "a Comparable " + pathType, fieldType, dtoType);
            case EQ, NOT_EQ -> expect(isSameErasure(boxed(fieldType), pathType), path, fieldName,
                                      pathType.toString(), fieldType, dtoType);
        };
    }

    /**
     * @param type an accessor's return type
     * @return the boxed erasure of the type when it implements {@link Comparable}, or empty otherwise
     */
    public Optional<TypeMirror> comparableErasure(TypeMirror type) {
        TypeMirror erased = types.erasure(boxed(type));
        return isAssignableToAny(erased, List.of(comparableType)) ? Optional.of(erased) : Optional.empty();
    }

    /**
     * @param type an accessor's return type
     * @return true for {@link OffsetDateTime} and every {@link ChronoZonedDateTime}, whose values are equal
     * when they denote the same instant
     */
    public boolean isInstantBased(TypeMirror type) {
        return isAssignableToAny(boxed(type), instantTypes);
    }

    /**
     * @param type an accessor's return type
     * @return true for {@code double}, {@code float} and their boxed types
     */
    public boolean isFloatingPoint(TypeMirror type) {
        return isAssignableToAny(boxed(type), floatingPointTypes);
    }

    private TypeMirror erasedType(Class<?> type) {
        return types.erasure(processingEnv.getElementUtils().getTypeElement(type.getCanonicalName()).asType());
    }

    private boolean isAssignableToAny(TypeMirror type, List<TypeMirror> erasedSupertypes) {
        TypeMirror erased = types.erasure(type);
        return erasedSupertypes.stream().anyMatch(supertype -> types.isAssignable(erased, supertype));
    }

    private boolean expect(boolean compatible, String path, String fieldName, String expected, TypeMirror actual,
                           TypeElement dtoType) {
        return compatible || error(ProcessorErrorMessageFactory.buildTypeMismatchMessage(path, fieldName, expected,
                                                                                         actual.toString()), dtoType);
    }

    private boolean error(String message, TypeElement dtoType) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, dtoType);
        return false;
    }

    private TypeMirror boxed(TypeMirror type) {
        return type.getKind().isPrimitive() ? types.boxedClass((PrimitiveType) type).asType() : type;
    }

    private boolean isSameErasure(TypeMirror first, TypeMirror second) {
        return types.isSameType(types.erasure(first), types.erasure(second));
    }

    private boolean isString(TypeMirror type) {
        return isDeclaredAs(type, String.class.getName());
    }

    private boolean isBoolean(TypeMirror type) {
        return isDeclaredAs(type, Boolean.class.getName());
    }

    private boolean isDeclaredAs(TypeMirror type, String qualifiedName) {
        return type.getKind() == TypeKind.DECLARED
                && MoreTypes.asTypeElement(type).getQualifiedName().contentEquals(qualifiedName);
    }

    /**
     * Finds the element type the type supplies to {@link Collection}, searching its supertypes; the upper bound
     * stands for a wildcard element type.
     *
     * @return the element type, or empty when the type is no collection, a raw one, or has an unbounded wildcard
     * element type
     */
    private Optional<TypeMirror> collectionElementType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return Optional.empty();
        }
        if (types.isSameType(types.erasure(type), collectionType)) {
            List<? extends TypeMirror> arguments = ((DeclaredType) type).getTypeArguments();
            if (arguments.size() != 1) {
                return Optional.empty();
            }
            TypeMirror element = arguments.get(0);
            return element.getKind() == TypeKind.WILDCARD
                    ? Optional.ofNullable(((WildcardType) element).getExtendsBound())
                    : Optional.of(element);
        }
        return types.directSupertypes(type).stream()
                    .map(this::collectionElementType)
                    .flatMap(Optional::stream)
                    .findFirst();
    }
}
