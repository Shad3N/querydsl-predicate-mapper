package io.github.shad3n.predicatemapper;

import com.google.auto.common.MoreTypes;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

/**
 * Resolves properties against a QueryDSL Q-Class hierarchy.
 */
class QClassPathResolver {

    private final ProcessingEnvironment processingEnv;

    public QClassPathResolver(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    /**
     * Resolves a dot-separated path string against a QueryDSL Q-class hierarchy.
     *
     * @param qClass       the base QueryDSL Q-class element
     * @param path         the dot-separated field path (e.g., "owner.name")
     * @param dtoType      the DTO type element (used for error reporting)
     * @param dtoFieldName the DTO field name (used for error reporting)
     * @return the variable element representing the ultimate field, or null if invalid
     */
    public VariableElement resolvePath(TypeElement qClass, String path, TypeElement dtoType, String dtoFieldName) {
        String[] segments = path.split("\\.");
        TypeElement current = qClass;
        VariableElement found = null;

        for (int i = 0; i < segments.length; i++) {
            String seg = segments[i];
            found = findFieldInHierarchy(current, seg);
            if (found == null) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                         ProcessorErrorMessageFactory.buildQClassPathInvalidMessage(
                                                                 path, seg,
                                                                 current.getQualifiedName().toString(),
                                                                 dtoFieldName,
                                                                 dtoType.getQualifiedName().toString()),
                                                         dtoType);
                return null;
            }
            if (i < segments.length - 1) {
                TypeMirror ft = found.asType();
                if (ft.getKind() != TypeKind.DECLARED) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                             ProcessorErrorMessageFactory.buildQClassPathSegmentNotTraversableMessage(
                                                                     path, seg),
                                                             dtoType);
                    return null;
                }
                current = MoreTypes.asTypeElement(ft);
            }
        }
        return found;
    }

    /**
     * Searches for a field by name within the given type element's mapped members.
     */
    private VariableElement findFieldInHierarchy(TypeElement type, String name) {
        return processingEnv.getElementUtils().getAllMembers(type).stream()
                            .filter(el -> el.getKind() == ElementKind.FIELD && el.getSimpleName().contentEquals(name))
                            .map(el -> (VariableElement) el)
                            .findFirst()
                            .orElse(null);
    }
}

