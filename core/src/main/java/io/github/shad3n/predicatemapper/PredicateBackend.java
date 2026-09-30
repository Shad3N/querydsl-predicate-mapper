package io.github.shad3n.predicatemapper;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import io.github.shad3n.predicatemapper.annotation.Op;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;

/**
 * Kind of predicate a mapper method produces: how filter paths resolve against its target, which
 * DTO field types fit them, and the code implementing the method. Every validation method reports its
 * own compilation errors.
 */
interface PredicateBackend {

    /**
     * Character making the next character of a LIKE pattern match itself, in every backend.
     */
    char LIKE_ESCAPE = '!';

    /**
     * @return the method annotation selecting this backend; its {@code value} names the target class
     */
    Class<? extends Annotation> methodAnnotation();

    /**
     * @param target the class the mapper method's annotation names
     * @return the return type the mapper method has to declare
     */
    TypeName returnType(ClassName target);

    /**
     * @param op the operator of a mapped DTO field
     * @return why this backend cannot map the operator, or empty when it can
     */
    Optional<String> rejectOperator(Op op);

    /**
     * Resolves a dot-separated path against the target, ending at the type of the value the path reaches.
     *
     * @param target   the target class element
     * @param path     the path declared by {@code @FilterField}
     * @param dtoField the DTO field declaring the path, for error reporting
     * @param dtoType  the DTO type, for error reporting
     * @return the resolved path, or null when the path is invalid
     */
    ResolvedPath resolvePath(TypeElement target, String path, VariableElement dtoField, TypeElement dtoType);

    /**
     * Generates the implementations of the given methods and any helpers they share.
     *
     * @param methods the methods this backend generates, in declaration order
     * @return the method specifications to add to the implementation class
     */
    List<MethodSpec> implement(List<MethodMapping> methods);
}
