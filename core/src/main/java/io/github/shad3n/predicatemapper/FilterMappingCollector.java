package io.github.shad3n.predicatemapper;

import com.google.auto.common.MoreElements;
import io.github.shad3n.predicatemapper.annotation.ToPredicate;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import java.util.List;

/**
 * Gathers and validates mappings from DTOs to QueryDSL predicates.
 * Processes elements annotated with {@code @io.github.shad3n.annotation.ToPredicate} to generate predicate mappings.
 */
class FilterMappingCollector {

    private final ProcessingEnvironment processingEnv;
    private final MethodMappingProcessor methodProcessor;

    /**
     * Constructs a new mapping collector.
     *
     * @param processingEnv the annotation processing environment
     */
    public FilterMappingCollector(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
        QClassPathResolver pathResolver = new QClassPathResolver(processingEnv);
        TypeCompatibilityChecker typeChecker = new TypeCompatibilityChecker(processingEnv);
        this.methodProcessor = new MethodMappingProcessor(processingEnv, pathResolver, typeChecker);
    }

    /**
     * Collects method mappings from the given interface annotated with {@code @io.github.shad3n.annotation.PredicateMapper}.
     *
     * @param iface the interface element to inspect
     * @return a list of collected method mappings, or null to signal deferral to next round
     */
    public List<MethodMapping> collect(TypeElement iface) {
        try {
            return MoreElements.getLocalAndInheritedMethods(iface, processingEnv.getTypeUtils(),
                                                            processingEnv.getElementUtils())
                               .stream()
                               .filter(m -> m.getAnnotation(ToPredicate.class) != null)
                               .map(methodProcessor::process)
                               .filter(java.util.Optional::isPresent)
                               .map(java.util.Optional::get)
                               .toList();
        } catch (DeferRoundException e) {
            return null;
        }
    }
}
