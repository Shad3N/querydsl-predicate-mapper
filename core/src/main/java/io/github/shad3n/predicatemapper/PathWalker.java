package io.github.shad3n.predicatemapper;

import com.google.auto.common.MoreTypes;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Follows a dot-separated path from a type, one member per segment, reporting an error on the DTO field when a
 * segment does not resolve or the type it reaches cannot be traversed further.
 */
class PathWalker {

    /**
     * A member one path segment resolves to.
     *
     * @param name the member's name in generated code
     * @param type the type the member yields on its owner
     */
    record Member(String name, TypeMirror type) {
    }

    /**
     * Resolves one path segment against the type the previous segments reached.
     */
    @FunctionalInterface
    interface MemberResolver {
        Optional<Member> resolve(DeclaredType owner, String segment);
    }

    /**
     * Builds the error message for a segment that does not resolve.
     */
    @FunctionalInterface
    interface MissingMemberMessage {
        String build(String segment, TypeElement owner);
    }

    private final ProcessingEnvironment processingEnv;

    public PathWalker(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    /**
     * Resolves every segment of the path in turn, starting from the given type.
     *
     * @param start          the type the path starts from
     * @param path           the dot-separated path (e.g., "owner.name")
     * @param dtoField       the DTO field declaring the path, where errors are reported
     * @param resolver       resolves each segment to a member
     * @param missingMessage builds the error for a segment that does not resolve
     * @return the member names and the last member's type, or null when the path is invalid
     */
    public ResolvedPath walk(TypeElement start, String path, VariableElement dtoField, MemberResolver resolver,
                             MissingMemberMessage missingMessage) {
        String[] segments = path.split("\\.");
        DeclaredType owner = MoreTypes.asDeclared(start.asType());
        List<String> members = new ArrayList<>();
        TypeMirror endType = null;

        for (int index = 0; index < segments.length; index++) {
            String segment = segments[index];
            Optional<Member> member = resolver.resolve(owner, segment);
            if (member.isEmpty()) {
                error(missingMessage.build(segment, MoreTypes.asTypeElement(owner)), dtoField);
                return null;
            }
            members.add(member.get().name());
            endType = member.get().type();
            if (index < segments.length - 1) {
                if (endType.getKind() != TypeKind.DECLARED) {
                    error(ProcessorErrorMessageFactory.buildPathSegmentNotTraversableMessage(path, segment), dtoField);
                    return null;
                }
                owner = MoreTypes.asDeclared(endType);
            }
        }
        return new ResolvedPath(List.copyOf(members), endType);
    }

    private void error(String message, VariableElement dtoField) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, dtoField);
    }
}
