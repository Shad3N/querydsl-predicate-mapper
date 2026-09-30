package io.github.shad3n.predicatemapper;

import com.google.auto.common.MoreTypes;
import io.github.shad3n.predicatemapper.annotation.FilterField;
import io.github.shad3n.predicatemapper.annotation.Op;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * Decides which types a {@code @FilterField} operator accepts, identically for every backend: the type of the
 * value the path reaches, and the type the DTO accessor returns. Primitive and boxed forms of a type are equal.
 */
class OperatorRules {

    private static final Set<Op> CASE_AWARE_OPERATORS = EnumSet.of(Op.EQ, Op.NOT_EQ, Op.LIKE, Op.CONTAINS, Op.REGEX);
    private static final Set<Op> NULL_CHECK_OPERATORS = EnumSet.of(Op.IS_NULL, Op.IS_NOT_NULL);

    private final ProcessingEnvironment processingEnv;
    private final Types types;
    private final TypeMirror comparableType;
    private final TypeMirror collectionType;

    public OperatorRules(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.types = processingEnv.getTypeUtils();
        this.comparableType = erasedType(Comparable.class);
        this.collectionType = erasedType(Collection.class);
    }

    /**
     * Checks a DTO field against its operator. {@code ignoreCase} needs EQ, NOT_EQ, LIKE, CONTAINS or REGEX on a
     * String path. Null checks need a Boolean value and a path that is not primitive. Every other operator needs a
     * value that is not primitive, so it can be left unset, and: a collection whose element type is the path type
     * for IN, a String on both sides for LIKE, CONTAINS and REGEX, a Comparable path of the value's type for LTE
     * and GTE, and the path type itself for EQ and NOT_EQ.
     *
     * @param dtoField   the {@code @FilterField} DTO field, where errors are reported
     * @param pathType   the type of the value the path reaches
     * @param getterName the name of the DTO accessor, for error messages
     * @param valueType  the type the DTO accessor returns
     * @return true when every rule holds, false after reporting the first rule broken
     */
    public boolean check(VariableElement dtoField, TypeMirror pathType, String getterName, TypeMirror valueType) {
        FilterField filterField = dtoField.getAnnotation(FilterField.class);
        String path = filterField.path();
        Op op = filterField.op();
        String fieldName = dtoField.getSimpleName().toString();
        TypeMirror boxedPathType = boxed(pathType);

        if (filterField.ignoreCase() && !CASE_AWARE_OPERATORS.contains(op)) {
            return error(ProcessorErrorMessageFactory.buildIgnoreCaseOperatorMessage(path, fieldName, op.name()),
                         dtoField);
        }
        if (filterField.ignoreCase() && !isString(boxedPathType)) {
            return error(ProcessorErrorMessageFactory.buildIgnoreCaseTypeMessage(path, fieldName, pathType.toString()),
                         dtoField);
        }
        if (!NULL_CHECK_OPERATORS.contains(op) && valueType.getKind().isPrimitive()) {
            return error(ProcessorErrorMessageFactory.buildPrimitiveFilterValueMessage(path, fieldName, getterName,
                                                                                       valueType.toString()),
                         dtoField);
        }

        return switch (op) {
            case IS_NULL, IS_NOT_NULL -> {
                if (pathType.getKind().isPrimitive()) {
                    yield error(ProcessorErrorMessageFactory.buildNullCheckOnPrimitiveMessage(
                            path, fieldName, op.name(), pathType.toString()), dtoField);
                }
                yield expect(isBoolean(boxed(valueType)), "java.lang.Boolean", valueType, dtoField);
            }
            case IN -> expect(SupertypeArguments.of(types, valueType, collectionType)
                                                .filter(element -> isSameErasure(element, boxedPathType))
                                                .isPresent(),
                              "java.util.Collection<" + boxedPathType + ">", valueType, dtoField);
            case LIKE, CONTAINS, REGEX -> {
                if (!isString(boxedPathType)) {
                    yield error(ProcessorErrorMessageFactory.buildTextOperatorTypeMessage(
                            path, fieldName, op.name(), pathType.toString()), dtoField);
                }
                yield expect(isString(valueType), "java.lang.String", valueType, dtoField);
            }
            case LTE, GTE -> expect(types.isAssignable(types.erasure(boxedPathType), comparableType)
                                            && isSameErasure(valueType, boxedPathType),
                                    "a Comparable " + boxedPathType, valueType, dtoField);
            case EQ, NOT_EQ -> expect(isSameErasure(valueType, boxedPathType), boxedPathType.toString(),
                                      valueType, dtoField);
        };
    }

    private TypeMirror erasedType(Class<?> type) {
        return types.erasure(processingEnv.getElementUtils().getTypeElement(type.getCanonicalName()).asType());
    }

    private boolean expect(boolean compatible, String expected, TypeMirror actual, VariableElement dtoField) {
        if (compatible) {
            return true;
        }
        FilterField filterField = dtoField.getAnnotation(FilterField.class);
        return error(ProcessorErrorMessageFactory.buildTypeMismatchMessage(
                filterField.path(), dtoField.getSimpleName().toString(), expected, actual.toString()), dtoField);
    }

    private boolean error(String message, VariableElement dtoField) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, dtoField);
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

    private static boolean isDeclaredAs(TypeMirror type, String qualifiedName) {
        return type.getKind() == TypeKind.DECLARED
                && MoreTypes.asTypeElement(type).getQualifiedName().contentEquals(qualifiedName);
    }
}
