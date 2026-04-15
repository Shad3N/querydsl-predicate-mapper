package io.github.shad3n.predicatemapper;

import com.google.auto.common.MoreTypes;
import io.github.shad3n.predicatemapper.annotation.Op;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.Collection;
import java.util.List;

/**
 * Validates the type compatibility between DTO fields and the target QueryDSL paths.
 */
class TypeCompatibilityChecker {

    private final ProcessingEnvironment processingEnv;

    public TypeCompatibilityChecker(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    /**
     * Checks if the type of DTO field is compatible with the target QueryDSL type.
     *
     * @param qField   the resolved QueryDSL field element
     * @param dtoField the DTO field element
     * @param path     the declared path for error messaging
     * @param dtoType  the parent DTO type for error anchoring
     * @param op       the operator to use for the field
     * @return true if compatible or unknown, false if clearly incompatible
     */
    public boolean check(VariableElement qField, VariableElement dtoField, String path, TypeElement dtoType, Op op) {
        TypeMirror qType = qField.asType();
        if (qType.getKind() != TypeKind.DECLARED) {
            return true;
        }

        String qTypeName = MoreTypes.asTypeElement(qType).getQualifiedName().toString();
        TypeMirror expected = mapQTypeToDtoType(qType, qTypeName, op);
        if (expected == null) {
            return true; // unknown Q-type, skip check
        }

        TypeMirror actual = dtoField.asType();

        // For IN operator, accept any Collection subtype
        if (op == Op.IN) {
            if (isCollectionType(actual)) {
                return true;
            }
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                     ProcessorErrorMessageFactory.buildTypeMismatchMessage(path,
                                                                                                           dtoField.getSimpleName()
                                                                                                                   .toString(),
                                                                                                           expected.toString(),
                                                                                                           actual.toString()),
                                                     dtoType);
            return false;
        }

        // For null-check operators, accept Boolean or boolean
        if (op == Op.IS_NULL || op == Op.IS_NOT_NULL) {
            if (isBooleanType(actual)) {
                return true;
            }
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                     ProcessorErrorMessageFactory.buildTypeMismatchMessage(path,
                                                                                                           dtoField.getSimpleName()
                                                                                                                   .toString(),
                                                                                                           "java.lang.Boolean",
                                                                                                           actual.toString()),
                                                     dtoType);
            return false;
        }

        // Standard type comparison
        String expectedStr = stripGenerics(expected.toString());
        String actualStr = stripGenerics(actual.toString());
        if (!actualStr.equals(expectedStr)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                     ProcessorErrorMessageFactory.buildTypeMismatchMessage(path,
                                                                                                           dtoField.getSimpleName()
                                                                                                                   .toString(),
                                                                                                           expectedStr,
                                                                                                           actual.toString()),
                                                     dtoType);
            return false;
        }
        return true;
    }

    /**
     * Checks if the type is a Collection or its subtype (List, Set, etc.)
     */
    private boolean isCollectionType(TypeMirror type) {
        if (type.getKind() != TypeKind.DECLARED) {
            return false;
        }
        TypeElement element = MoreTypes.asTypeElement(type);
        String name = element.getQualifiedName().toString();
        // Accept Collection, List, Set, and their subtypes
        return name.equals("java.util.Collection") ||
                name.equals("java.util.List") ||
                name.equals("java.util.Set") ||
                name.startsWith("java.util.") && isSubtypeOfCollection(type);
    }

    /**
     * Checks if type is a subtype of Collection using type utils.
     */
    private boolean isSubtypeOfCollection(TypeMirror type) {
        TypeMirror collectionType = processingEnv.getElementUtils().getTypeElement(Collection.class.getName()).asType();
        return processingEnv.getTypeUtils().isSubtype(
                processingEnv.getTypeUtils().erasure(type),
                processingEnv.getTypeUtils().erasure(collectionType));
    }

    /**
     * Checks if the type is Boolean or boolean.
     */
    private boolean isBooleanType(TypeMirror type) {
        if (type.getKind() == TypeKind.BOOLEAN) {
            return true;
        }
        if (type.getKind() == TypeKind.DECLARED) {
            String name = MoreTypes.asTypeElement(type).getQualifiedName().toString();
            return name.equals("java.lang.Boolean");
        }
        return false;
    }

    /**
     * Maps a QueryDSL path type to its corresponding standard Java type, accounting for the operator.
     */
    private TypeMirror mapQTypeToDtoType(TypeMirror qType, String qTypeName, Op op) {
        // Null-check operators expect Boolean DTO fields
        if (op == Op.IS_NULL || op == Op.IS_NOT_NULL) {
            return processingEnv.getElementUtils().getTypeElement("java.lang.Boolean").asType();
        }

        List<? extends TypeMirror> args = MoreTypes.asDeclared(qType).getTypeArguments();
        TypeMirror first = args.isEmpty() ? null : args.get(0);

        // IN operator expects Collection<T> when Q-path is T
        if (op == Op.IN) {
            return processingEnv.getElementUtils().getTypeElement(Collection.class.getName()).asType();
        }

        String baseType = switch (qTypeName) {
            case "com.querydsl.core.types.dsl.StringPath" -> "java.lang.String";
            case "com.querydsl.core.types.dsl.BooleanPath" -> "java.lang.Boolean";
            case "com.querydsl.core.types.dsl.NumberPath", "com.querydsl.core.types.dsl.DatePath",
                 "com.querydsl.core.types.dsl.DateTimePath", "com.querydsl.core.types.dsl.TimePath",
                 "com.querydsl.core.types.dsl.EnumPath", "com.querydsl.core.types.dsl.ComparablePath" ->
                    first != null ? first.toString() : null;
            case "com.querydsl.core.types.dsl.ListPath", "com.querydsl.core.types.dsl.CollectionPath",
                 "com.querydsl.core.types.dsl.SetPath" -> "java.util.List";
            default -> null;
        };

        if (baseType == null) {
            return null;
        }

        TypeElement element = processingEnv.getElementUtils().getTypeElement(baseType);
        return element != null ? element.asType() : null;
    }

    /**
     * Strips generic type parameters from a type name string.
     */
    private String stripGenerics(String typeName) {
        int lt = typeName.indexOf('<');
        return lt < 0 ? typeName : typeName.substring(0, lt);
    }
}

