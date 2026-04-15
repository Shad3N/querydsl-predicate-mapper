package io.github.shad3n.predicatemapper;

import com.google.auto.common.AnnotationMirrors;
import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.palantir.javapoet.ClassName;
import io.github.shad3n.predicatemapper.annotation.FilterField;
import io.github.shad3n.predicatemapper.annotation.Op;
import io.github.shad3n.predicatemapper.annotation.ToPredicate;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates and processes a single method mapping.
 */
class MethodMappingProcessor {

    private final ProcessingEnvironment processingEnv;
    private final QClassPathResolver pathResolver;
    private final TypeCompatibilityChecker typeChecker;

    public MethodMappingProcessor(ProcessingEnvironment processingEnv, QClassPathResolver pathResolver,
                                  TypeCompatibilityChecker typeChecker) {
        this.processingEnv = processingEnv;
        this.pathResolver = pathResolver;
        this.typeChecker = typeChecker;
    }

    /**
     * Processes a single method mapping, verifying parameters and resolving Q-classes.
     *
     * @param method the method element to process
     * @return an optional containing the resolved mapping, or empty if validation fails
     */
    public java.util.Optional<MethodMapping> process(ExecutableElement method) {
        TypeElement dtoElement = extractDtoElement(method);
        if (dtoElement == null) {
            return java.util.Optional.empty();
        }

        TypeElement qClassElement = extractQClassElement(method);

        List<FieldMapping> fieldMappings = collectMappings(dtoElement, qClassElement);
        if (fieldMappings == null) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(new MethodMapping(method.getSimpleName().toString(), ClassName.get(qClassElement),
                                                       ClassName.get(dtoElement), fieldMappings));
    }

    /**
     * Extracts and validates the DTO parameter element from the method.
     *
     * @param method the method element
     * @return the DTO type element, or null if invalid
     */
    private TypeElement extractDtoElement(ExecutableElement method) {
        List<? extends VariableElement> parameters = method.getParameters();
        if (parameters.size() != 1) {
            error(ProcessorErrorMessageFactory.buildToPredicateOneParamMessage(), method);
            return null;
        }

        TypeMirror dtoClassMirror = parameters.get(0).asType();
        if (dtoClassMirror.getKind() != TypeKind.DECLARED) {
            error(ProcessorErrorMessageFactory.buildToPredicateParamClassMessage(), method);
            return null;
        }

        return MoreTypes.asTypeElement(dtoClassMirror);
    }

    /**
     * Extracts and validates the QueryDSL Q-class element from the method's annotation.
     *
     * @param method the method element
     * @return the Q-class type element
     * @throws DeferRoundException if the Q-class is not yet available
     */
    private TypeElement extractQClassElement(ExecutableElement method) {
        TypeMirror qClassMirror = getQClassMirror(method);
        if (qClassMirror == null) {
            throw new DeferRoundException();
        }
        if (qClassMirror.getKind() != TypeKind.DECLARED) {
            error(ProcessorErrorMessageFactory.buildCannotResolveQClassMessage(), method);
            throw new DeferRoundException();
        }
        return MoreTypes.asTypeElement(qClassMirror);
    }

    /**
     * Retrieves the Q-class TypeMirror from the @io.github.shad3n.annotation.ToPredicate annotation.
     *
     * @param method the method element
     * @return the type mirror, or null if invalid or unresolved
     */
    private TypeMirror getQClassMirror(ExecutableElement method) {
        com.google.common.base.Optional<AnnotationMirror> annotationMirror =
                MoreElements.getAnnotationMirror(method, ToPredicate.class);
        if (!annotationMirror.isPresent()) {
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
            error(ProcessorErrorMessageFactory.buildCannotReadToPredicateValueMessage(method.getSimpleName().toString(),
                                                                                      value.getClass().getSimpleName(),
                                                                                      String.valueOf(value)), method);
            return null;
        }
    }

    /**
     * Collects all field mappings for a specific DTO against a Q-class.
     *
     * @param dtoElement    the DTO type element
     * @param qClassElement the QueryDSL Q-class type element
     * @return a list of field mappings, or null if any validation error occurred
     */
    private List<FieldMapping> collectMappings(TypeElement dtoElement, TypeElement qClassElement) {
        List<VariableElement> annotatedFields = dtoElement.getEnclosedElements().stream()
                                                          .filter(e -> e.getKind() == ElementKind.FIELD)
                                                          .map(e -> (VariableElement) e)
                                                          .filter(e -> e.getAnnotation(FilterField.class) != null)
                                                          .toList();

        List<FieldMapping> mappings = new ArrayList<>();
        for (VariableElement dtoField : annotatedFields) {
            FieldMapping mapping = processSingleFieldMapping(dtoField, dtoElement, qClassElement);
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

        return new FieldMapping(dtoField.getSimpleName().toString(), path, operation);
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
