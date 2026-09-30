package io.github.shad3n.predicatemapper;

import com.google.auto.common.MoreElements;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Gathers and validates the mapper methods of a {@code @PredicateMapper} interface, pairing each with the
 * backend its mapper annotation selects.
 */
class FilterMappingCollector {

    private final ProcessingEnvironment processingEnv;
    private final MethodMappingProcessor methodProcessor;
    private final List<PredicateBackend> backends;

    /**
     * Constructs a new mapping collector.
     *
     * @param processingEnv the annotation processing environment
     */
    public FilterMappingCollector(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        this.methodProcessor = new MethodMappingProcessor(processingEnv);
        this.backends = List.of(new QueryDslBackend(processingEnv), new JavaPredicateBackend(processingEnv));
    }

    /**
     * Collects method mappings from the given interface annotated with {@code @PredicateMapper}.
     *
     * @param iface the interface element to inspect
     * @return a list of collected method mappings, or null to signal deferral to next round
     */
    public List<MethodMapping> collect(TypeElement iface) {
        try {
            return MoreElements.getLocalAndInheritedMethods(iface, processingEnv.getTypeUtils(),
                                                            processingEnv.getElementUtils())
                               .stream()
                               .flatMap(this::process)
                               .toList();
        } catch (DeferRoundException e) {
            return null;
        }
    }

    private Stream<MethodMapping> process(ExecutableElement method) {
        List<PredicateBackend> selected = backends.stream()
                                                  .filter(b -> method.getAnnotation(b.methodAnnotation()) != null)
                                                  .toList();
        if (selected.isEmpty() && method.getModifiers().contains(Modifier.ABSTRACT)) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    ProcessorErrorMessageFactory.buildMissingMapperAnnotationMessage(
                            method.getSimpleName().toString()),
                    method);
            return Stream.empty();
        }
        if (selected.size() > 1) {
            processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.ERROR,
                    ProcessorErrorMessageFactory.buildConflictingMapperAnnotationsMessage(
                            method.getSimpleName().toString()),
                    method);
            return Stream.empty();
        }
        return selected.stream()
                       .map(backend -> methodProcessor.process(method, backend))
                       .flatMap(Optional::stream);
    }
}
