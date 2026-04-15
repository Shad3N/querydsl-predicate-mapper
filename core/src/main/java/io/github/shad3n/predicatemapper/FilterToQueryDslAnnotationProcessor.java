package io.github.shad3n.predicatemapper;

import com.google.auto.service.AutoService;
import io.github.shad3n.predicatemapper.annotation.PredicateMapper;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Main entry point for the Filter to QueryDSL Annotation Processor.
 * It processes {@code @io.github.shad3n.annotation.PredicateMapper} interfaces and generates Spring Component implementations
 * to map DTOs into QueryDSL Predicates.
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("io.github.shad3n.predicatemapper.annotation.PredicateMapper")
public class FilterToQueryDslAnnotationProcessor extends AbstractProcessor {

    // We need to deffer until other processors have completed
    // to have access to Q-classes of QueryDSL
    private final Set<TypeElement> deferredElements = new LinkedHashSet<>();

    /**
     * Required by AbstractProcessor to specify supported Java versions.
     *
     * @return the latest supported
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * Processes the annotations for the current round.
     *
     * @param annotations the annotation types requested to be processed
     * @param roundEnv    environment for information about the current and prior round
     * @return true if the annotations are claimed by this processor, false otherwise
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Init with previously skipped elements
        Set<TypeElement> elementsToProcess = new LinkedHashSet<>(deferredElements);
        deferredElements.clear();

        roundEnv.getElementsAnnotatedWith(PredicateMapper.class).stream()
                .filter(this::isValidInterface)
                .map(TypeElement.class::cast)
                .forEach(elementsToProcess::add);

        elementsToProcess.forEach(iface -> {
            try {
                if (!processMapperInterface(iface)) {
                    deferredElements.add(iface);
                }
            } catch (IOException e) {
                handleGenerationError(iface, e);
            }
        });

        checkUnresolvedElements(roundEnv);

        return true;
    }

    private boolean isValidInterface(Element el) {
        if (el.getKind() != ElementKind.INTERFACE) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                     ProcessorErrorMessageFactory.buildNonInterfaceMapperMessage(), el);
            return false;
        }
        return true;
    }

    private void handleGenerationError(TypeElement iface, IOException e) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                 ProcessorErrorMessageFactory.buildGenerationFailedMessage(
                                                         e.getMessage()), iface);
    }

    /**
     * Checks if there are any interfaces left unprocessed at the end of the annotation processing rounds.
     * This typically means a Q-class could not be resolved.
     *
     * @param roundEnv the environment for information about the current round
     */
    private void checkUnresolvedElements(RoundEnvironment roundEnv) {
        if (roundEnv.processingOver() && !deferredElements.isEmpty()) {
            deferredElements.forEach(el ->
                                             processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                                                      ProcessorErrorMessageFactory.buildUnresolvedQClassMessage(),
                                                                                      el)
            );
        }
    }

    /**
     * Parses the given target interface and delegates generation of its implementation.
     *
     * @param iface the interface element annotated with {@code @io.github.shad3n.annotation.PredicateMapper}
     * @return true if successfully processed, false if it needs to be deferred to the next round
     * @throws IOException if an error occurs while writing the generated source file
     */
    private boolean processMapperInterface(TypeElement iface) throws IOException {
        FilterMappingCollector collector = new FilterMappingCollector(processingEnv);
        List<MethodMapping> methods = collector.collect(iface);

        if (methods == null) {
            return false; // Signal to defer to next round
        }

        if (!methods.isEmpty()) {
            FilterImplementationGenerator generator = new FilterImplementationGenerator(processingEnv);
            generator.generateImpl(iface, methods);
        }

        return true;
    }
}
