package io.github.shad3n.predicatemapper;

import com.google.auto.common.MoreTypes;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Resolves dot-separated paths against plain Java types through their public accessor methods.
 */
class AccessorPathResolver {

    private final ProcessingEnvironment processingEnv;

    public AccessorPathResolver(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    /**
     * Resolves each path segment to a public, non-static, no-argument accessor, trying the record accessor
     * form {@code segment()} first, then {@code getSegment()}, then {@code isSegment()}.
     *
     * @param target       the class the path starts from
     * @param path         the dot-separated path (e.g., "owner.name")
     * @param dtoType      the DTO type element (used for error reporting)
     * @param dtoFieldName the DTO field name (used for error reporting)
     * @return the accessor names and the last accessor's return type, or null if invalid
     */
    public ResolvedPath resolvePath(TypeElement target, String path, TypeElement dtoType, String dtoFieldName) {
        String[] segments = path.split("\\.");
        DeclaredType current = MoreTypes.asDeclared(target.asType());
        List<String> accessors = new ArrayList<>();
        TypeMirror endType = null;

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            Optional<ExecutableElement> accessor = findAccessor(MoreTypes.asTypeElement(current), segment);
            if (accessor.isEmpty()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                         ProcessorErrorMessageFactory.buildAccessorPathInvalidMessage(
                                                                 path, segment, capitalize(segment),
                                                                 MoreTypes.asTypeElement(current).getQualifiedName()
                                                                          .toString(),
                                                                 dtoFieldName,
                                                                 dtoType.getQualifiedName().toString()),
                                                         dtoType);
                return null;
            }
            accessors.add(accessor.get().getSimpleName().toString());
            endType = MoreTypes.asExecutable(processingEnv.getTypeUtils().asMemberOf(current, accessor.get()))
                               .getReturnType();
            if (i < segments.length - 1) {
                if (endType.getKind() != TypeKind.DECLARED) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                             ProcessorErrorMessageFactory.buildPathSegmentNotTraversableMessage(
                                                                     path, segment),
                                                             dtoType);
                    return null;
                }
                current = MoreTypes.asDeclared(endType);
            }
        }
        return new ResolvedPath(List.copyOf(accessors), endType);
    }

    private Optional<ExecutableElement> findAccessor(TypeElement type, String segment) {
        List<ExecutableElement> candidates = processingEnv.getElementUtils().getAllMembers(type).stream()
                                                          .filter(e -> e.getKind() == ElementKind.METHOD)
                                                          .map(ExecutableElement.class::cast)
                                                          .filter(m -> m.getParameters().isEmpty()
                                                                  && m.getModifiers().contains(Modifier.PUBLIC)
                                                                  && !m.getModifiers().contains(Modifier.STATIC)
                                                                  && m.getReturnType().getKind() != TypeKind.VOID)
                                                          .toList();
        String capitalized = capitalize(segment);
        return List.of(segment, "get" + capitalized, "is" + capitalized).stream()
                   .flatMap(name -> candidates.stream().filter(m -> m.getSimpleName().contentEquals(name)))
                   .findFirst();
    }

    private static String capitalize(String segment) {
        return segment.isEmpty() ? segment : Character.toUpperCase(segment.charAt(0)) + segment.substring(1);
    }
}
