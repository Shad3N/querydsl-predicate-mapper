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
import java.util.List;
import java.util.Optional;

/**
 * Validates and processes a single mapper method against the backend its annotation selects.
 */
class MethodMappingProcessor {

    private final ProcessingEnvironment processingEnv;
    private final AccessorLookup accessors;
    private final OperatorRules operatorRules;

    public MethodMappingProcessor(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.accessors = new AccessorLookup(processingEnv);
        this.operatorRules = new OperatorRules(processingEnv);
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
     * @param backend       the backend resolving paths
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
     * Collects the {@code @FilterField} fields the DTO declares or inherits, superclass fields first.
     *
     * @param dtoElement the DTO type element
     * @return the annotated fields, in declaration order within each class
     */
    private List<VariableElement> annotatedFields(TypeElement dtoElement) {
        List<TypeElement> hierarchy = new ArrayList<>();
        for (TypeElement type = dtoElement; type != null; type = superclassOf(type)) {
            hierarchy.add(0, type);
        }
        return hierarchy.stream()
                        .flatMap(type -> type.getEnclosedElements().stream())
                        .filter(e -> e.getKind() == ElementKind.FIELD)
                        .map(VariableElement.class::cast)
                        .filter(e -> e.getAnnotation(FilterField.class) != null)
                        .toList();
    }

    private static TypeElement superclassOf(TypeElement type) {
        TypeMirror superclass = type.getSuperclass();
        return superclass.getKind() == TypeKind.DECLARED ? MoreTypes.asTypeElement(superclass) : null;
    }

    /**
     * Processes a single field mapping by checking the backend supports its operator, resolving its path and DTO
     * accessor, and checking both types against the operator.
     *
     * @param method        the mapper method, for error reporting
     * @param dtoField      the DTO field element
     * @param dtoElement    the DTO type element
     * @param targetElement the target type element
     * @param backend       the backend resolving the path
     * @return the resolved field mapping, or null if validation fails
     */
    private FieldMapping processSingleFieldMapping(ExecutableElement method, VariableElement dtoField,
                                                   TypeElement dtoElement, TypeElement targetElement,
                                                   PredicateBackend backend) {
        FilterField filterField = dtoField.getAnnotation(FilterField.class);
        String path = filterField.path();
        Op operation = filterField.op();
        String dtoFieldName = dtoField.getSimpleName().toString();

        Optional<String> rejection = backend.rejectOperator(operation);
        if (rejection.isPresent()) {
            String annotationName = backend.methodAnnotation().getSimpleName();
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    ProcessorErrorMessageFactory.buildUnsupportedOperatorMessage(
                            annotationName, method.getSimpleName().toString(), operation.name(), dtoFieldName,
                            dtoElement.getQualifiedName().toString(), rejection.get()),
                    method,
                    MoreElements.getAnnotationMirror(method, backend.methodAnnotation()).orNull());
            return null;
        }

        ResolvedPath target = backend.resolvePath(targetElement, path, dtoField, dtoElement);
        if (target == null) {
            return null;
        }

        Optional<ExecutableElement> getter = dtoAccessor(filterField, dtoFieldName, dtoElement);
        if (getter.isEmpty()) {
            String candidates = filterField.getter().isEmpty()
                    ? AccessorLookup.describePropertyAccessors(dtoFieldName)
                    : "'" + filterField.getter() + "()'";
            error(ProcessorErrorMessageFactory.buildMissingDtoAccessorMessage(
                    dtoFieldName, dtoElement.getQualifiedName().toString(), candidates), dtoField);
            return null;
        }
        String getterName = getter.get().getSimpleName().toString();
        TypeMirror getterType = MoreTypes.asExecutable(
                processingEnv.getTypeUtils().asMemberOf(MoreTypes.asDeclared(dtoElement.asType()), getter.get()))
                                         .getReturnType();
        if (!operatorRules.check(dtoField, target.endType(), getterName, getterType)) {
            return null;
        }

        return new FieldMapping(dtoFieldName, operation, filterField.ignoreCase(), getterName, target);
    }

    /**
     * Finds the accessor reading a DTO field: the {@code FilterField.getter()} override if set, otherwise the
     * accessor {@link AccessorLookup#forProperty} finds for the field's name.
     *
     * @param filterField  the annotation on the DTO field
     * @param dtoFieldName the DTO field's name
     * @param dtoElement   the DTO type element
     * @return the accessor, or empty when the DTO has none
     */
    private Optional<ExecutableElement> dtoAccessor(FilterField filterField, String dtoFieldName,
                                                    TypeElement dtoElement) {
        String override = filterField.getter();
        return override.isEmpty()
                ? accessors.forProperty(dtoElement, dtoFieldName)
                : accessors.byName(dtoElement, override);
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
