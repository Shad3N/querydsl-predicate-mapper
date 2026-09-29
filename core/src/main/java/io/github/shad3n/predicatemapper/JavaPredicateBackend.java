package io.github.shad3n.predicatemapper;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.NameAllocator;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.github.shad3n.predicatemapper.annotation.Op;
import io.github.shad3n.predicatemapper.annotation.ToJavaPredicateMapper;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Maps filter DTOs to {@link Predicate} implementations testing plain Java objects through their accessors.
 * Generated code references JDK types only.
 */
class JavaPredicateBackend implements PredicateBackend {

    private static final ClassName PREDICATE = ClassName.get(Predicate.class);
    private static final String LIKE_PATTERN_METHOD = "likePattern";
    private static final String REGEX_PATTERN_METHOD = "regexPattern";
    private static final String TIMED_REGEX_INPUT_METHOD = "timedRegexInput";
    private static final int REGEX_MATCH_SECONDS = 1;
    private static final CodeBlock CASE_INSENSITIVE_FLAGS =
            CodeBlock.of("$T.CASE_INSENSITIVE | $T.UNICODE_CASE", Pattern.class, Pattern.class);

    private final AccessorPathResolver pathResolver;
    private final JavaTypeCompatibilityChecker typeChecker;

    public JavaPredicateBackend(ProcessingEnvironment processingEnv) {
        this.pathResolver = new AccessorPathResolver(processingEnv);
        this.typeChecker = new JavaTypeCompatibilityChecker(processingEnv);
    }

    @Override
    public Class<? extends Annotation> methodAnnotation() {
        return ToJavaPredicateMapper.class;
    }

    @Override
    public TypeName returnType(ClassName target) {
        return ParameterizedTypeName.get(PREDICATE, target);
    }

    @Override
    public Optional<String> rejectOperator(Op op) {
        return Optional.empty();
    }

    @Override
    public ResolvedPath resolvePath(TypeElement target, String path, VariableElement dtoField, TypeElement dtoType) {
        return pathResolver.resolvePath(target, path, dtoType, dtoField.getSimpleName().toString());
    }

    @Override
    public boolean isCompatible(ResolvedPath target, String path, VariableElement dtoField, TypeElement dtoType, Op op,
                                boolean ignoreCase) {
        return typeChecker.check(target, path, dtoField, dtoType, op, ignoreCase);
    }

    @Override
    public List<MethodSpec> implement(List<MethodMapping> methods) {
        List<MethodSpec> generated = new ArrayList<>();
        methods.forEach(methodMapping -> generated.add(generateMethod(methodMapping)));
        Set<Op> operators = methods.stream()
                                   .flatMap(methodMapping -> methodMapping.fields().stream())
                                   .map(FieldMapping::op)
                                   .collect(Collectors.toCollection(() -> EnumSet.noneOf(Op.class)));
        if (operators.contains(Op.LIKE)) {
            generated.add(likePatternMethod());
        }
        if (operators.contains(Op.REGEX)) {
            generated.add(regexPatternMethod());
            generated.add(timedRegexInputMethod());
        }
        return generated;
    }

    /**
     * Generates a method starting from an always-true predicate and and-ing one condition per set DTO field.
     * Each field's getter is read once when the predicate is built; patterns are compiled once per predicate.
     */
    private MethodSpec generateMethod(MethodMapping methodMapping) {
        NameAllocator names = new NameAllocator();
        String dto = names.newName("dto");
        String predicate = names.newName("predicate");
        String targetParameter = names.newName("target");

        TypeName predicateType = returnType(methodMapping.target());
        MethodSpec.Builder method = MethodSpec.methodBuilder(methodMapping.methodName())
                                              .addAnnotation(Override.class)
                                              .addModifiers(Modifier.PUBLIC)
                                              .returns(predicateType)
                                              .addParameter(methodMapping.dtoClass(), dto)
                                              .addStatement("$T $N = $N -> true", predicateType, predicate,
                                                            targetParameter)
                                              .beginControlFlow("if ($N == null)", dto)
                                              .addStatement("return $N", predicate)
                                              .endControlFlow();

        for (FieldMapping field : methodMapping.fields()) {
            String filterValue = names.newName(field.dtoFieldName());
            method.addStatement("var $N = $N.$N()", filterValue, dto, field.getterName());

            boolean nullCheck = field.op() == Op.IS_NULL || field.op() == Op.IS_NOT_NULL;
            if (nullCheck) {
                method.beginControlFlow("if ($T.TRUE.equals($N))", Boolean.class, filterValue);
            } else {
                method.beginControlFlow("if ($N != null)", filterValue);
            }

            String pattern = null;
            if (usesPattern(field)) {
                pattern = names.newName(field.dtoFieldName() + "Pattern");
                method.addStatement("var $N = $L", pattern, compilePattern(field, filterValue));
            }

            FieldCode fieldCode = new FieldCode(field, filterValue, pattern, comparisonFor(field));
            method.addCode(CodeBlock.builder()
                                    .add("$N = $N.and($N -> {\n", predicate, predicate, targetParameter)
                                    .indent()
                                    .add(fieldTest(fieldCode, targetParameter, names.clone()))
                                    .unindent()
                                    .add("});\n")
                                    .build());
            method.endControlFlow();
        }

        return method.addStatement("return $N", predicate).build();
    }

    /**
     * Local names and comparison strategy the generated code of one DTO field works with.
     *
     * @param mapping     the field mapping
     * @param filterValue name of the local holding the filter value
     * @param pattern     name of the local holding the compiled pattern, or null when the operator uses none
     * @param comparison  how filter and target values compare
     */
    private record FieldCode(FieldMapping mapping, String filterValue, String pattern, Comparison comparison) {
    }

    /**
     * Builds the lambda body testing one target against a field's condition. Every object along a nested
     * path has to be present, otherwise the target fails the condition, whatever its operator.
     */
    private CodeBlock fieldTest(FieldCode fieldCode, String target, NameAllocator names) {
        CodeBlock.Builder body = CodeBlock.builder();
        List<String> accessors = fieldCode.mapping().target().members();
        String owner = target;
        for (String accessor : accessors.subList(0, accessors.size() - 1)) {
            String member = names.newName("member");
            body.addStatement("var $N = $N.$N()", member, owner, accessor)
                .beginControlFlow("if ($N == null)", member)
                .addStatement("return false")
                .endControlFlow();
            owner = member;
        }
        String value = names.newName("value");
        return body.addStatement("var $N = $N.$N()", value, owner, accessors.get(accessors.size() - 1))
                   .addStatement("return $L", condition(fieldCode, value, names))
                   .build();
    }

    /**
     * Tells whether the field's condition matches through a compiled {@link Pattern}: always for LIKE and REGEX,
     * and for CONTAINS only when ignoring case.
     */
    private static boolean usesPattern(FieldMapping field) {
        return switch (field.op()) {
            case LIKE, REGEX -> true;
            case CONTAINS -> field.ignoreCase();
            default -> false;
        };
    }

    /**
     * Builds the expression compiling a field's pattern: LIKE through the LIKE translator, case-insensitive
     * CONTAINS as a quoted literal, and REGEX through the helper reporting invalid expressions.
     */
    private CodeBlock compilePattern(FieldMapping field, String filterValue) {
        return switch (field.op()) {
            case LIKE -> CodeBlock.of("$N($N, $L)", LIKE_PATTERN_METHOD, filterValue, field.ignoreCase());
            case CONTAINS -> CodeBlock.of("$T.compile($T.quote($N), $L)", Pattern.class, Pattern.class, filterValue,
                                          CASE_INSENSITIVE_FLAGS);
            case REGEX -> CodeBlock.of("$N($N, $L, $S)", REGEX_PATTERN_METHOD, filterValue,
                                       field.ignoreCase() ? CASE_INSENSITIVE_FLAGS : CodeBlock.of("0"),
                                       field.dtoFieldName());
            default -> throw new IllegalArgumentException("Operator compiles no pattern: " + field.op());
        };
    }

    /**
     * Builds the boolean expression a target value has to satisfy, following SQL's null rules: only
     * {@link Op#IS_NULL} holds for a null value.
     */
    private CodeBlock condition(FieldCode fieldCode, String value, NameAllocator names) {
        FieldMapping field = fieldCode.mapping();
        return switch (field.op()) {
            case IS_NULL -> CodeBlock.of("$N == null", value);
            case IS_NOT_NULL -> CodeBlock.of("$N != null", value);
            default -> field.target().endType().getKind().isPrimitive()
                    ? valueTest(fieldCode, value, names)
                    : CodeBlock.of("$N != null && $L", value, valueTest(fieldCode, value, names));
        };
    }

    /**
     * Builds the test a non-null target value has to pass under a value operator.
     */
    private CodeBlock valueTest(FieldCode fieldCode, String value, NameAllocator names) {
        FieldMapping field = fieldCode.mapping();
        String filterValue = fieldCode.filterValue();
        String pattern = fieldCode.pattern();
        return switch (field.op()) {
            case EQ, NOT_EQ, LTE, GTE ->
                    fieldCode.comparison().test(field.op(), CodeBlock.of("$N", filterValue), value);
            case IN -> membership(fieldCode, value, names);
            case LIKE -> CodeBlock.of("$N.matcher($N).matches()", pattern, value);
            case CONTAINS -> field.ignoreCase()
                    ? CodeBlock.of("$N.matcher($N).find()", pattern, value)
                    : CodeBlock.of("$N.contains($N)", value, filterValue);
            case REGEX -> CodeBlock.of("$N.matcher($N($N)).find()", pattern, TIMED_REGEX_INPUT_METHOD, value);
            case IS_NULL, IS_NOT_NULL -> throw new IllegalArgumentException("Not a value operator: " + field.op());
        };
    }

    private Comparison comparisonFor(FieldMapping field) {
        TypeMirror endType = field.target().endType();
        if (field.ignoreCase()) {
            return Comparison.IGNORING_CASE;
        }
        if (typeChecker.isInstantBased(endType)) {
            return Comparison.SAME_INSTANT;
        }
        if (typeChecker.isFloatingPoint(endType)) {
            return Comparison.FLOATING_POINT;
        }
        return typeChecker.comparableErasure(endType).isPresent() ? Comparison.COMPARE_TO : Comparison.EQUALS;
    }

    /**
     * Builds a test for a filter collection holding an element equal to the value under the field's
     * {@link Comparison}; null elements never match.
     */
    private CodeBlock membership(FieldCode fieldCode, String value, NameAllocator names) {
        if (fieldCode.comparison() == Comparison.EQUALS) {
            return CodeBlock.of("$N.contains($N)", fieldCode.filterValue(), value);
        }
        String candidate = names.newName("candidate");
        TypeName elementType =
                TypeName.get(typeChecker.comparableErasure(fieldCode.mapping().target().endType()).orElseThrow());
        CodeBlock typedCandidate = CodeBlock.of("(($T) $N)", elementType, candidate);
        return CodeBlock.of("$N.stream().anyMatch($N -> $N != null && $L)", fieldCode.filterValue(), candidate,
                            candidate, fieldCode.comparison().test(Op.EQ, typedCandidate, value));
    }

    /**
     * How a filter value compares to a target value, matching SQL: case-insensitive text through
     * {@code equalsIgnoreCase}; offset and zoned date-times by instant; {@code Double} and {@code Float} by
     * numeric value, so {@code 0.0} equals {@code -0.0}; other Comparable values through {@code compareTo};
     * anything else through {@code equals}. Only the Comparable strategies order values.
     */
    private enum Comparison {
        IGNORING_CASE("$L.equalsIgnoreCase($N)", "!$L.equalsIgnoreCase($N)", null, null),
        SAME_INSTANT("$L.isEqual($N)", "!$L.isEqual($N)", "!$L.isBefore($N)", "!$L.isAfter($N)"),
        FLOATING_POINT("$L.doubleValue() == $N", "$L.doubleValue() != $N", "$L.doubleValue() >= $N",
                       "$L.doubleValue() <= $N"),
        COMPARE_TO("$L.compareTo($N) == 0", "$L.compareTo($N) != 0", "$L.compareTo($N) >= 0",
                   "$L.compareTo($N) <= 0"),
        EQUALS("$L.equals($N)", "!$L.equals($N)", null, null);

        private final String equalFormat;
        private final String unequalFormat;
        private final String atMostFormat;
        private final String atLeastFormat;

        Comparison(String equalFormat, String unequalFormat, String atMostFormat, String atLeastFormat) {
            this.equalFormat = equalFormat;
            this.unequalFormat = unequalFormat;
            this.atMostFormat = atMostFormat;
            this.atLeastFormat = atLeastFormat;
        }

        /**
         * Builds the test the target value has to pass against the filter value.
         *
         * @param op     EQ or IN for equality, NOT_EQ for inequality, LTE for a target at most the filter
         *               value, GTE for a target at least the filter value
         * @param filter expression yielding the filter value
         * @param value  name of the target value
         */
        CodeBlock test(Op op, CodeBlock filter, String value) {
            String format = switch (op) {
                case EQ, IN -> equalFormat;
                case NOT_EQ -> unequalFormat;
                case LTE -> atMostFormat;
                case GTE -> atLeastFormat;
                default -> throw new IllegalArgumentException("Not a comparison operator: " + op);
            };
            return CodeBlock.of(format, filter, value);
        }
    }

    /**
     * Generates a helper translating a SQL LIKE pattern into a {@link Pattern} matching whole values:
     * {@code %} matches any run of characters, {@code _} any single character, the LIKE escape character makes
     * the character after it match itself, and everything else matches itself.
     */
    private MethodSpec likePatternMethod() {
        return MethodSpec.methodBuilder(LIKE_PATTERN_METHOD)
                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                         .returns(Pattern.class)
                         .addParameter(String.class, "like")
                         .addParameter(boolean.class, "ignoreCase")
                         .addStatement("$T regex = new $T()", StringBuilder.class, StringBuilder.class)
                         .addStatement("$T literal = new $T()", StringBuilder.class, StringBuilder.class)
                         .beginControlFlow("for (int index = 0; index < like.length(); index++)")
                         .addStatement("char character = like.charAt(index)")
                         .beginControlFlow("if (character == '$L' && index + 1 < like.length())", LIKE_ESCAPE)
                         .addStatement("index++")
                         .addStatement("literal.append(like.charAt(index))")
                         .nextControlFlow("else if (character == '%' || character == '_')")
                         .addStatement("regex.append($T.quote(literal.toString())).append(character == '%' ? \".*\" : \".\")",
                                       Pattern.class)
                         .addStatement("literal.setLength(0)")
                         .nextControlFlow("else")
                         .addStatement("literal.append(character)")
                         .endControlFlow()
                         .endControlFlow()
                         .addStatement("regex.append($T.quote(literal.toString()))", Pattern.class)
                         .addStatement("return $T.compile(regex.toString(), $T.DOTALL | (ignoreCase ? $L : 0))",
                                       Pattern.class, Pattern.class, CASE_INSENSITIVE_FLAGS)
                         .build();
    }

    /**
     * Generates a helper compiling a REGEX filter value, rejecting an invalid expression with an
     * {@link IllegalArgumentException} naming the DTO field.
     */
    private MethodSpec regexPatternMethod() {
        return MethodSpec.methodBuilder(REGEX_PATTERN_METHOD)
                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                         .returns(Pattern.class)
                         .addParameter(String.class, "regex")
                         .addParameter(int.class, "flags")
                         .addParameter(String.class, "field")
                         .beginControlFlow("try")
                         .addStatement("return $T.compile(regex, flags)", Pattern.class)
                         .nextControlFlow("catch ($T exception)", PatternSyntaxException.class)
                         .addStatement("throw new $T($S + field + $S + exception.getDescription(), exception)",
                                       IllegalArgumentException.class, "REGEX filter value of '",
                                       "' is not a valid regular expression: ")
                         .endControlFlow()
                         .build();
    }

    /**
     * Generates a helper wrapping a target value so that a REGEX match reading it fails with an
     * {@link IllegalStateException} once it runs past its time limit, instead of backtracking unbounded.
     */
    private MethodSpec timedRegexInputMethod() {
        TypeSpec timedText = TypeSpec.anonymousClassBuilder("")
                                     .addSuperinterface(CharSequence.class)
                                     .addMethod(MethodSpec.methodBuilder("charAt")
                                                          .addAnnotation(Override.class)
                                                          .addModifiers(Modifier.PUBLIC)
                                                          .returns(char.class)
                                                          .addParameter(int.class, "index")
                                                          .beginControlFlow("if ($T.nanoTime() > deadline)",
                                                                            System.class)
                                                          .addStatement("throw new $T($S)",
                                                                        IllegalStateException.class,
                                                                        "REGEX match exceeded its "
                                                                                + REGEX_MATCH_SECONDS
                                                                                + " s time limit")
                                                          .endControlFlow()
                                                          .addStatement("return text.charAt(index)")
                                                          .build())
                                     .addMethod(MethodSpec.methodBuilder("length")
                                                          .addAnnotation(Override.class)
                                                          .addModifiers(Modifier.PUBLIC)
                                                          .returns(int.class)
                                                          .addStatement("return text.length()")
                                                          .build())
                                     .addMethod(MethodSpec.methodBuilder("subSequence")
                                                          .addAnnotation(Override.class)
                                                          .addModifiers(Modifier.PUBLIC)
                                                          .returns(CharSequence.class)
                                                          .addParameter(int.class, "start")
                                                          .addParameter(int.class, "end")
                                                          .addStatement("return text.subSequence(start, end)")
                                                          .build())
                                     .addMethod(MethodSpec.methodBuilder("toString")
                                                          .addAnnotation(Override.class)
                                                          .addModifiers(Modifier.PUBLIC)
                                                          .returns(String.class)
                                                          .addStatement("return text.toString()")
                                                          .build())
                                     .build();
        return MethodSpec.methodBuilder(TIMED_REGEX_INPUT_METHOD)
                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                         .returns(CharSequence.class)
                         .addParameter(CharSequence.class, "text")
                         .addStatement("long deadline = $T.nanoTime() + $T.SECONDS.toNanos($L)", System.class,
                                       TimeUnit.class, REGEX_MATCH_SECONDS)
                         .addStatement("return $L", timedText)
                         .build();
    }
}
