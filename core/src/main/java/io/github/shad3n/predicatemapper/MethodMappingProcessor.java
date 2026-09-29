package io.github.shad3n.predicatemapper;

import com.google.auto.common.AnnotationMirrors;
import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import io.github.shad3n.predicatemapper.annotation.FilterField;
import io.github.shad3n.predicatemapper.annotation.Op;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Validates and processes a single mapper method against the backend its annotation selects.
 */
class MethodMappingProcessor {

    private static final Set<Op> CASE_AWARE_OPERATORS = EnumSet.of(Op.EQ, Op.NOT_EQ, Op.LIKE, Op.CONTAINS, Op.REGEX);

    private static final Set<Op> NULL_CHECK_OPERATORS = EnumSet.of(Op.IS_NULL, Op.IS_NOT_NULL);

    private final ProcessingEnvironment processingEnv;

    public MethodMappingProcessor(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    /**
     * Processes a single method mapping, verifying its parameter, target class, return type and fields.
     *
     * @param method  the method element to process
     * @param backend the backend selected by the method's annotation
     * @return an optional containing the resolved mapping, or empty if validation fails
     * @throws DeferRoundException if the target class is not yet available (unresolved type)
     */
    public Optional<MethodMapping> process(ExecutableElement method, PredicateBackend backend) {
        String annotationName = backend.methodAnnotation().getSimpleName();

        TypeElement dtoElement = extractDtoElement(method, annotationName);
        if (dtoElement == null) {
            return Optional.empty();
        }

        TypeElement targetElement = extractTargetElement(method, backend);
        if (targetElement == null) {
            return Optional.empty();
        }

        ClassName target = ClassName.get(targetElement);
        if (!hasExpectedReturnType(method, backend.returnType(target), annotationName)) {
            return Optional.empty();
        }

        List<FieldMapping> fieldMappings = collectMappings(method, dtoElement, targetElement, backend);
        if (fieldMappings == null) {
            return Optional.empty();
        }

        return Optional.of(new MethodMapping(method.getSimpleName().toString(), backend, target,
                                             ClassName.get(dtoElement), fieldMappings));
    }

    /**
     * Extracts and validates the DTO parameter element from the method.
     *
     * @param method         the method element
     * @param annotationName the simple name of the method's mapper annotation, for error messages
     * @return the DTO type element, or null if invalid
     */
    private TypeElement extractDtoElement(ExecutableElement method, String annotationName) {
        List<? extends VariableElement> parameters = method.getParameters();
        if (parameters.size() != 1) {
            error(ProcessorErrorMessageFactory.buildMapperOneParamMessage(annotationName), method);
            return null;
        }

        TypeMirror dtoClassMirror = parameters.get(0).asType();
        if (dtoClassMirror.getKind() != TypeKind.DECLARED) {
            error(ProcessorErrorMessageFactory.buildMapperParamClassMessage(annotationName), method);
            return null;
        }

        return MoreTypes.asTypeElement(dtoClassMirror);
    }

    /**
     * Extracts and validates the target class element named by the method's mapper annotation.
     *
     * @param method  the method element
     * @param backend the backend whose annotation names the target
     * @return the target type element, or null if it is a real error (not a deferral)
     * @throws DeferRoundException if the target class is not yet available (unresolved type)
     */
    private TypeElement extractTargetElement(ExecutableElement method, PredicateBackend backend) {
        TypeMirror targetMirror = getTargetMirror(method, backend);
        if (targetMirror == null) {
            throw new DeferRoundException();
        }
        if (targetMirror.getKind() != TypeKind.DECLARED) {
            error(ProcessorErrorMessageFactory.buildCannotResolveTargetClassMessage(
                    backend.methodAnnotation().getSimpleName()), method);
            return null;
        }
        return MoreTypes.asTypeElement(targetMirror);
    }

    /**
     * Retrieves the target class TypeMirror from the method's mapper annotation.
     *
     * @param method  the method element
     * @param backend the backend whose annotation names the target
     * @return the type mirror, or null if invalid or unresolved
     */
    private TypeMirror getTargetMirror(ExecutableElement method, PredicateBackend backend) {
        Optional<AnnotationMirror> annotationMirror =
                MoreElements.getAnnotationMirror(method, backend.methodAnnotation()).toJavaUtil();
        if (annotationMirror.isEmpty()) {
            return null;
        }
        AnnotationValue annotationValue = AnnotationMirrors.getAnnotationValue(annotationMirror.get(), "value");
        Object value = annotationValue.getValue();
        if (value instanceof TypeMirror typeMirror) {
            if (typeMirror.getKind() == TypeKind.ERROR) {
                return null;
            }
            return typeMirror;
        } else if ("<error>".equals(value)) {
            return null;
        } else {
            error(ProcessorErrorMessageFactory.buildCannotReadMapperValueMessage(
                    backend.methodAnnotation().getSimpleName(), method.getSimpleName().toString(),
                    value.getClass().getSimpleName(), String.valueOf(value)), method);
            return null;
        }
    }

    private boolean hasExpectedReturnType(ExecutableElement method, TypeName expected, String annotationName) {
        TypeName actual = TypeName.get(method.getReturnType());
        if (actual.equals(expected)) {
            return true;
        }
        error(ProcessorErrorMessageFactory.buildWrongReturnTypeMessage(annotationName,
                                                                       method.getSimpleName().toString(),
                                                                       expected.toString(), actual.toString()),
              method);
        return false;
    }

    /**
     * Collects all field mappings for a specific DTO against the method's target.
     *
     * @param method        the mapper method, for error reporting
     * @param dtoElement    the DTO type element
     * @param targetElement the target type element
     * @param backend       the backend resolving paths and checking types
     * @return a list of field mappings, or null if any validation error occurred
     */
    private List<FieldMapping> collectMappings(ExecutableElement method, TypeElement dtoElement,
                                               TypeElement targetElement, PredicateBackend backend) {
        List<FieldMapping> mappings = new ArrayList<>();
        for (VariableElement dtoField : annotatedFields(dtoElement)) {
            FieldMapping mapping = processSingleFieldMapping(method, dtoField, dtoElement, targetElement, backend);
            if (mapping == null) {
                return null;
            }
            mappings.add(mapping);
        }
        return mappings;
    }

    /**
     * Processes a single field mapping by resolving its Q-class path and checking type compatibility.
     *
     * @param dtoField      the DTO field element
     * @param dtoElement    the DTO type element
     * @param qClassElement the QueryDSL Q-class type element
     * @return the resolved field mapping, or null if validation fails
     */
    private FieldMapping processSingleFieldMapping(VariableElement dtoField, TypeElement dtoElement,
                                                   TypeElement qClassElement) {
        FilterField filterField = dtoField.getAnnotation(FilterField.class);
        String path = filterField.path();
        Op operation = filterField.op();

        VariableElement qClassField =
                pathResolver.resolvePath(qClassElement, path, dtoElement, dtoField.getSimpleName().toString());
        if (qClassField == null || !typeChecker.check(qClassField, dtoField, path, dtoElement, operation)) {
            return null;
        }

        return new FieldMapping(dtoField.getSimpleName().toString(), path, operation,
                                resolveGetter(filterField, dtoField, dtoElement));
    }

    /**
     * Resolves the getter expression for a DTO field.
     * Uses {@code FilterField.getter()} override if set; otherwise tries {@code get*} then {@code is*}.
     *
     * @param filterField the annotation on the DTO field
     * @param dtoField    the DTO field element
     * @param dtoElement  the DTO type element
     * @return the getter expression (e.g., {@code "dto.getName()"})
     */
    private String resolveGetter(FilterField filterField, VariableElement dtoField, TypeElement dtoElement) {
        String override = filterField.getter();
        if (!override.isEmpty()) {
            return "dto." + override + "()";
        }
        String fieldName = dtoField.getSimpleName().toString();
        String capitalized = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        String getGetter = "get" + capitalized;
        String isGetter = "is" + capitalized;

        boolean hasGetGetter = processingEnv.getElementUtils().getAllMembers(dtoElement).stream()
                                            .anyMatch(e -> e.getKind() == ElementKind.METHOD
                                                    && e.getSimpleName().contentEquals(getGetter)
                                                    && ((ExecutableElement) e).getParameters().isEmpty());
        if (hasGetGetter) {
            return "dto." + getGetter + "()";
        }

        boolean hasIsGetter = processingEnv.getElementUtils().getAllMembers(dtoElement).stream()
                                           .anyMatch(e -> e.getKind() == ElementKind.METHOD
                                                   && e.getSimpleName().contentEquals(isGetter)
                                                   && ((ExecutableElement) e).getParameters().isEmpty());
        return "dto." + (hasIsGetter ? isGetter : getGetter) + "()";
    }

    /**
     * Emits a compilation error attached to a specific AST element.
     *
     * @param msg the internal error message to display
     * @param el  the AST element causally related to the error
     */
    private void error(String msg, Element el) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, el);
    }
}
