package io.github.shad3n.predicatemapper;

import com.google.auto.common.MoreTypes;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Types;

/**
 * Resolves dot-separated paths against plain Java types through their accessor methods.
 */
class AccessorPathResolver {

    private final PathWalker walker;
    private final AccessorLookup accessors;
    private final Types types;

    public AccessorPathResolver(ProcessingEnvironment processingEnv) {
        this.walker = new PathWalker(processingEnv);
        this.accessors = new AccessorLookup(processingEnv);
        this.types = processingEnv.getTypeUtils();
    }

    /**
     * Resolves each path segment to the accessor {@link AccessorLookup#forProperty} finds for it.
     *
     * @param target   the class the path starts from
     * @param path     the dot-separated path (e.g., "owner.name")
     * @param dtoField the DTO field declaring the path, for error reporting
     * @param dtoType  the DTO type, for error reporting
     * @return the accessor names and the last accessor's return type, or null if invalid
     */
    public ResolvedPath resolvePath(TypeElement target, String path, VariableElement dtoField, TypeElement dtoType) {
        return walker.walk(
                target, path, dtoField,
                (owner, segment) -> accessors.forProperty(MoreTypes.asTypeElement(owner), segment).map(
                        accessor -> new PathWalker.Member(
                                accessor.getSimpleName().toString(),
                                MoreTypes.asExecutable(types.asMemberOf(owner, accessor)).getReturnType())),
                (segment, owner) -> ProcessorErrorMessageFactory.buildAccessorPathInvalidMessage(
                        path, AccessorLookup.describePropertyAccessors(segment),
                        owner.getQualifiedName().toString(), dtoField.getSimpleName().toString(),
                        dtoType.getQualifiedName().toString()));
    }
}
