package io.github.shad3n.predicatemapper;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Finds the accessors generated code reads values through: public, non-static, no-argument methods returning a
 * value, declared or inherited by a type, excluding those {@link Object} declares and their overrides.
 */
class AccessorLookup {

    private final Elements elements;
    private final Set<String> objectMethodNames;

    public AccessorLookup(ProcessingEnvironment processingEnv) {
        this.elements = processingEnv.getElementUtils();
        TypeElement object = elements.getTypeElement(Object.class.getCanonicalName());
        this.objectMethodNames = ElementFilter.methodsIn(object.getEnclosedElements()).stream()
                                              .filter(method -> method.getParameters().isEmpty())
                                              .map(method -> method.getSimpleName().toString())
                                              .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Finds the accessor with the given name.
     *
     * @param type the type declaring or inheriting the accessor
     * @param name the accessor's method name
     * @return the accessor, or empty when the type has none by that name
     */
    public Optional<ExecutableElement> byName(TypeElement type, String name) {
        if (objectMethodNames.contains(name)) {
            return Optional.empty();
        }
        return elements.getAllMembers(type).stream()
                       .filter(member -> member.getKind() == ElementKind.METHOD
                               && member.getSimpleName().contentEquals(name))
                       .map(ExecutableElement.class::cast)
                       .filter(AccessorLookup::isAccessor)
                       .findFirst();
    }

    /**
     * Finds the accessor of a property, trying {@code property()}, then {@code getProperty()}, then
     * {@code isProperty()}.
     *
     * @param type     the type declaring or inheriting the accessor
     * @param property the property name
     * @return the first accessor found, or empty when the type has none of them
     */
    public Optional<ExecutableElement> forProperty(TypeElement type, String property) {
        return propertyAccessorNames(property).map(name -> byName(type, name))
                                              .flatMap(Optional::stream)
                                              .findFirst();
    }

    /**
     * Describes the accessor names {@link #forProperty} tries, for error messages.
     *
     * @param property the property name
     * @return the names as quoted method calls, such as {@code 'name()', 'getName()' or 'isName()'}
     */
    public static String describePropertyAccessors(String property) {
        String capitalized = capitalize(property);
        return "'%s()', 'get%s()' or 'is%s()'".formatted(property, capitalized, capitalized);
    }

    private static Stream<String> propertyAccessorNames(String property) {
        String capitalized = capitalize(property);
        return Stream.of(property, "get" + capitalized, "is" + capitalized);
    }

    private static String capitalize(String name) {
        return name.isEmpty() ? name : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static boolean isAccessor(ExecutableElement method) {
        return method.getModifiers().contains(Modifier.PUBLIC)
                && !method.getModifiers().contains(Modifier.STATIC)
                && method.getParameters().isEmpty()
                && method.getReturnType().getKind() != TypeKind.VOID;
    }
}
