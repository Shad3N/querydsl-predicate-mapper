package io.github.shad3n.predicatemapper;

final class ProcessorErrorMessageFactory {

    private static final String NON_INTERFACE_MAPPER =
            "@io.github.shad3n.annotation.PredicateMapper must be placed on an interface";
    private static final String GENERATION_FAILED = "Failed to generate mapper implementation: %s";
    private static final String UNRESOLVED_Q_CLASS =
            "Cannot resolve Q-class in @io.github.shad3n.annotation.ToPredicate (unresolved after final compilation round)";
    private static final String TO_PREDICATE_ONE_PARAM =
            "@io.github.shad3n.annotation.ToPredicate method must have exactly one parameter";
    private static final String CANNOT_RESOLVE_Q_CLASS =
            "Cannot resolve Q-class in @io.github.shad3n.annotation.ToPredicate";
    private static final String TO_PREDICATE_PARAM_CLASS =
            "@io.github.shad3n.annotation.ToPredicate parameter must be a class type";
    private static final String CANNOT_READ_TO_PREDICATE_VALUE =
            "Cannot read @io.github.shad3n.annotation.ToPredicate value on '%s'. Expected a class reference but got %s ('%s').";
    private static final String Q_CLASS_PATH_INVALID =
            "Q-class path '%s' invalid: field '%s' not found in %s (referenced by @io.github.shad3n.annotation.FilterField on '%s' in %s)";
    private static final String Q_CLASS_PATH_SEGMENT_NOT_TRAVERSABLE =
            "Q-class path '%s': segment '%s' is not a traversable declared type";
    private static final String TYPE_MISMATCH =
            "Type mismatch for @io.github.shad3n.annotation.FilterField(path=\"%s\") on '%s': Q-path maps to %s but DTO field is %s";

    private ProcessorErrorMessageFactory() {
        // utility class
    }

    public static String buildNonInterfaceMapperMessage() {
        return NON_INTERFACE_MAPPER;
    }

    public static String buildGenerationFailedMessage(String details) {
        return String.format(GENERATION_FAILED, details);
    }

    public static String buildUnresolvedQClassMessage() {
        return UNRESOLVED_Q_CLASS;
    }

    public static String buildToPredicateOneParamMessage() {
        return TO_PREDICATE_ONE_PARAM;
    }

    public static String buildCannotResolveQClassMessage() {
        return CANNOT_RESOLVE_Q_CLASS;
    }

    public static String buildToPredicateParamClassMessage() {
        return TO_PREDICATE_PARAM_CLASS;
    }

    public static String buildCannotReadToPredicateValueMessage(String methodName, String actualClass,
                                                                String actualValue) {
        return String.format(CANNOT_READ_TO_PREDICATE_VALUE, methodName, actualClass, actualValue);
    }

    public static String buildQClassPathInvalidMessage(String path, String field, String qClass, String dtoField,
                                                       String dtoClass) {
        return String.format(Q_CLASS_PATH_INVALID, path, field, qClass, dtoField, dtoClass);
    }

    public static String buildQClassPathSegmentNotTraversableMessage(String path, String segment) {
        return String.format(Q_CLASS_PATH_SEGMENT_NOT_TRAVERSABLE, path, segment);
    }

    public static String buildTypeMismatchMessage(String path, String fieldName, String expectedType,
                                                  String actualType) {
        return String.format(TYPE_MISMATCH, path, fieldName, expectedType, actualType);
    }
}
