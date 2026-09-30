package io.github.shad3n.predicatemapper;

import com.google.auto.common.MoreTypes;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Optional;

/**
 * Resolves properties against a QueryDSL Q-Class hierarchy.
 */
class QClassPathResolver {

    private static final String QUERYDSL_EXPRESSION = "com.querydsl.core.types.Expression";

    private final PathWalker walker;
    private final Elements elements;
    private final Types types;

    public QClassPathResolver(ProcessingEnvironment processingEnv) {
        this.walker = new PathWalker(processingEnv);
        this.elements = processingEnv.getElementUtils();
        this.types = processingEnv.getTypeUtils();
    }

    /**
     * Resolves each segment of a dot-separated path to the Q-class field of that name, ending at the type of the
     * value the last field's QueryDSL expression yields.
     *
     * @param qClass   the base QueryDSL Q-class element
     * @param path     the dot-separated field path (e.g., "owner.name")
     * @param dtoField the DTO field declaring the path, for error reporting
     * @param dtoType  the DTO type, for error reporting
     * @return the field names and the value type, or null if invalid
     */
    public ResolvedPath resolvePath(TypeElement qClass, String path, VariableElement dtoField, TypeElement dtoType) {
        ResolvedPath qPath = walker.walk(
                qClass, path, dtoField,
                (owner, segment) -> field(MoreTypes.asTypeElement(owner), segment).map(
                        field -> new PathWalker.Member(segment, field.asType())),
                (segment, owner) -> ProcessorErrorMessageFactory.buildQClassPathInvalidMessage(
                        path, segment, owner.getQualifiedName().toString(), dtoField.getSimpleName().toString(),
                        dtoType.getQualifiedName().toString()));
        return qPath == null ? null : new ResolvedPath(qPath.members(), valueType(qPath.endType()));
    }

    private TypeMirror valueType(TypeMirror qType) {
        TypeMirror expressionType = types.erasure(elements.getTypeElement(QUERYDSL_EXPRESSION).asType());
        return SupertypeArguments.of(types, qType, expressionType).orElse(qType);
    }

    private Optional<VariableElement> field(TypeElement type, String name) {
        return elements.getAllMembers(type).stream()
                       .filter(member -> member.getKind() == ElementKind.FIELD
                               && member.getSimpleName().contentEquals(name))
                       .map(VariableElement.class::cast)
                       .findFirst();
    }
}
