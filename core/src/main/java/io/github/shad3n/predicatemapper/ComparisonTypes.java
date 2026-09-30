package io.github.shad3n.predicatemapper;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.time.OffsetDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Classifies the types a path reaches by how generated Java code compares their values. Primitive and boxed forms
 * of a type classify alike.
 */
class ComparisonTypes {

    private final ProcessingEnvironment processingEnv;
    private final Types types;
    private final TypeMirror comparableType;
    private final List<TypeMirror> instantTypes;
    private final List<TypeMirror> floatingPointTypes;

    public ComparisonTypes(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.types = processingEnv.getTypeUtils();
        this.comparableType = erasedType(Comparable.class);
        this.instantTypes = List.of(erasedType(OffsetDateTime.class), erasedType(ChronoZonedDateTime.class));
        this.floatingPointTypes = List.of(erasedType(Double.class), erasedType(Float.class));
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

    private TypeMirror boxed(TypeMirror type) {
        return type.getKind().isPrimitive() ? types.boxedClass((PrimitiveType) type).asType() : type;
    }
}
