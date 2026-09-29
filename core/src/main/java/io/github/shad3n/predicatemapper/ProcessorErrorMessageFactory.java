package io.github.shad3n.predicatemapper;

final class ProcessorErrorMessageFactory {

    private static final String NON_INTERFACE_MAPPER = "@PredicateMapper must be placed on an interface";
    private static final String GENERATION_FAILED = "Failed to generate mapper implementation: %s";
    private static final String UNRESOLVED_TARGET_CLASS =
            "Cannot resolve the target class of a mapper method (unresolved after final compilation round)";
    private static final String MAPPER_ONE_PARAM = "@%s method must have exactly one parameter";
    private static final String CANNOT_RESOLVE_TARGET_CLASS = "Cannot resolve the target class in @%s";
    private static final String MAPPER_PARAM_CLASS = "@%s parameter must be a class type";
    private static final String CANNOT_READ_MAPPER_VALUE =
            "Cannot read @%s value on '%s'. Expected a class reference but got %s ('%s').";
    private static final String CONFLICTING_MAPPER_ANNOTATIONS =
            "Method '%s' carries more than one mapper annotation; keep exactly one";
    private static final String MISSING_MAPPER_ANNOTATION =
            "Abstract method '%s' of a @PredicateMapper interface needs @ToQueryDslPredicateMapper or "
                    + "@ToJavaPredicateMapper";
    private static final String WRONG_RETURN_TYPE = "@%s method '%s' must return %s, but returns %s";
    private static final String UNSUPPORTED_OPERATOR =
            "@%s method '%s' cannot map Op.%s of field '%s' in %s: %s";
    private static final String Q_CLASS_PATH_INVALID =
            "Q-class path '%s' invalid: field '%s' not found in %s (referenced by @FilterField on '%s' in %s)";
    private static final String ACCESSOR_PATH_INVALID =
            "Path '%s' invalid: no public accessor '%s()', 'get%s()' or 'is%s()' in %s (referenced by @FilterField on '%s' in %s)";
    private static final String PATH_SEGMENT_NOT_TRAVERSABLE =
            "Path '%s': segment '%s' is not a traversable declared type";
    private static final String TYPE_MISMATCH =
            "Type mismatch for @FilterField(path=\"%s\") on '%s': path maps to %s but DTO field is %s";
    private static final String IGNORE_CASE_OPERATOR =
            "@FilterField(path=\"%s\") on '%s': ignoreCase applies to EQ, NOT_EQ, LIKE, CONTAINS and REGEX only, not %s";
    private static final String IGNORE_CASE_TYPE =
            "@FilterField(path=\"%s\") on '%s': ignoreCase needs a String path, but the path maps to %s";
    private static final String TEXT_OPERATOR_TYPE =
            "@FilterField(path=\"%s\") on '%s': %s needs a String path, but the path maps to %s";
    private static final String NULL_CHECK_ON_PRIMITIVE =
            "@FilterField(path=\"%s\") on '%s': %s cannot apply to the primitive %s, which is never null";

    private static final String PRIMITIVE_FILTER_VALUE =
            "@FilterField(path=\"%s\") on '%s': %s() returns the primitive %s, which can never be left unset; "
                    + "use its boxed type";

    private ProcessorErrorMessageFactory() {
        // utility class
    }

    public static String buildNonInterfaceMapperMessage() {
        return NON_INTERFACE_MAPPER;
    }

    public static String buildGenerationFailedMessage(String details) {
        return String.format(GENERATION_FAILED, details);
    }

    public static String buildUnresolvedTargetClassMessage() {
        return UNRESOLVED_TARGET_CLASS;
    }

    public static String buildMapperOneParamMessage(String annotation) {
        return String.format(MAPPER_ONE_PARAM, annotation);
    }

    public static String buildCannotResolveTargetClassMessage(String annotation) {
        return String.format(CANNOT_RESOLVE_TARGET_CLASS, annotation);
    }

    public static String buildMapperParamClassMessage(String annotation) {
        return String.format(MAPPER_PARAM_CLASS, annotation);
    }

    public static String buildCannotReadMapperValueMessage(String annotation, String methodName, String actualClass,
                                                           String actualValue) {
        return String.format(CANNOT_READ_MAPPER_VALUE, annotation, methodName, actualClass, actualValue);
    }

    public static String buildConflictingMapperAnnotationsMessage(String methodName) {
        return String.format(CONFLICTING_MAPPER_ANNOTATIONS, methodName);
    }

    public static String buildMissingMapperAnnotationMessage(String methodName) {
        return String.format(MISSING_MAPPER_ANNOTATION, methodName);
    }

    public static String buildWrongReturnTypeMessage(String annotation, String methodName, String expectedType,
                                                     String actualType) {
        return String.format(WRONG_RETURN_TYPE, annotation, methodName, expectedType, actualType);
    }

    public static String buildUnsupportedOperatorMessage(String annotation, String methodName, String op,
                                                         String dtoField, String dtoClass, String reason) {
        return String.format(UNSUPPORTED_OPERATOR, annotation, methodName, op, dtoField, dtoClass, reason);
    }

    public static String buildQClassPathInvalidMessage(String path, String field, String qClass, String dtoField,
                                                       String dtoClass) {
        return String.format(Q_CLASS_PATH_INVALID, path, field, qClass, dtoField, dtoClass);
    }

    public static String buildAccessorPathInvalidMessage(String path, String segment, String capitalizedSegment,
                                                         String targetClass, String dtoField, String dtoClass) {
        return String.format(ACCESSOR_PATH_INVALID, path, segment, capitalizedSegment, capitalizedSegment,
                             targetClass, dtoField, dtoClass);
    }

    public static String buildPathSegmentNotTraversableMessage(String path, String segment) {
        return String.format(PATH_SEGMENT_NOT_TRAVERSABLE, path, segment);
    }

    public static String buildTypeMismatchMessage(String path, String fieldName, String expectedType,
                                                  String actualType) {
        return String.format(TYPE_MISMATCH, path, fieldName, expectedType, actualType);
    }

    public static String buildIgnoreCaseOperatorMessage(String path, String fieldName, String op) {
        return String.format(IGNORE_CASE_OPERATOR, path, fieldName, op);
    }

    public static String buildIgnoreCaseTypeMessage(String path, String fieldName, String actualType) {
        return String.format(IGNORE_CASE_TYPE, path, fieldName, actualType);
    }

    public static String buildTextOperatorTypeMessage(String path, String fieldName, String op, String actualType) {
        return String.format(TEXT_OPERATOR_TYPE, path, fieldName, op, actualType);
    }

    public static String buildPrimitiveFilterValueMessage(String path, String fieldName, String getterName,
                                                          String primitiveType) {
        return String.format(PRIMITIVE_FILTER_VALUE, path, fieldName, getterName, primitiveType);
    }

    public static String buildNullCheckOnPrimitiveMessage(String path, String fieldName, String op,
                                                          String primitiveType) {
        return String.format(NULL_CHECK_ON_PRIMITIVE, path, fieldName, op, primitiveType);
    }
}
