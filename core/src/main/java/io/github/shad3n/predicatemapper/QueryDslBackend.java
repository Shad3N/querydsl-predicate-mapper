package io.github.shad3n.predicatemapper;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import io.github.shad3n.predicatemapper.annotation.Op;
import io.github.shad3n.predicatemapper.annotation.ToQueryDslPredicateMapper;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Maps filter DTOs to QueryDSL predicates built against a Q-class.
 */
class QueryDslBackend implements PredicateBackend {

    private static final ClassName PREDICATE = ClassName.get("com.querydsl.core.types", "Predicate");
    private static final ClassName BOOLEAN_BUILDER = ClassName.get("com.querydsl.core", "BooleanBuilder");
    private static final ClassName EXPRESSIONS = ClassName.get("com.querydsl.core.types.dsl", "Expressions");

    private final QClassPathResolver pathResolver;
    private final TypeCompatibilityChecker typeChecker;

    public QueryDslBackend(ProcessingEnvironment processingEnv) {
        this.pathResolver = new QClassPathResolver(processingEnv);
        this.typeChecker = new TypeCompatibilityChecker(processingEnv);
    }

    @Override
    public Class<? extends Annotation> methodAnnotation() {
        return ToQueryDslPredicateMapper.class;
    }

    @Override
    public TypeName returnType(ClassName target) {
        return PREDICATE;
    }

    @Override
    public Optional<String> rejectOperator(Op op) {
        return op == Op.REGEX
                ? Optional.of("QueryDSL's JPA templates rewrite regular expressions into LIKE, which fails or "
                                      + "silently mismatches at query time; map it with @ToJavaPredicateMapper instead")
                : Optional.empty();
    }

    @Override
    public ResolvedPath resolvePath(TypeElement target, String path, VariableElement dtoField, TypeElement dtoType) {
        VariableElement qField = pathResolver.resolvePath(target, path, dtoType, dtoField.getSimpleName().toString());
        return qField == null ? null : new ResolvedPath(List.of(path.split("\\.")), qField.asType());
    }

    @Override
    public boolean isCompatible(ResolvedPath target, String path, VariableElement dtoField, TypeElement dtoType, Op op,
                                boolean ignoreCase) {
        return typeChecker.check(target.endType(), dtoField, path, dtoType, op, ignoreCase);
    }

    @Override
    public List<MethodSpec> implement(List<MethodMapping> methods) {
        return methods.stream().map(this::generateMethod).toList();
    }

    /**
     * Generates a method and-ing one condition per set DTO field into a {@code BooleanBuilder}, and returning an
     * always-true predicate when no field is set or the DTO is null.
     */
    private MethodSpec generateMethod(MethodMapping methodMapping) {
        ClassName qClass = methodMapping.target();
        MethodSpec.Builder method = MethodSpec.methodBuilder(methodMapping.methodName())
                                              .addAnnotation(Override.class)
                                              .addModifiers(Modifier.PUBLIC)
                                              .returns(returnType(qClass))
                                              .addParameter(methodMapping.dtoClass(), "dto")
                                              .beginControlFlow("if (dto == null)")
                                              .addStatement("return $T.TRUE", EXPRESSIONS)
                                              .endControlFlow()
                                              .addStatement("$T q = new $T($S)", qClass, qClass,
                                                            entityVariableName(qClass))
                                              .addStatement("$T builder = new $T()", BOOLEAN_BUILDER,
                                                            BOOLEAN_BUILDER);

        methodMapping.fields().forEach(field -> addFieldCondition(method, field));

        return method.addStatement("$T result = builder.getValue()", PREDICATE)
                     .addStatement("return result != null ? result : $T.TRUE", EXPRESSIONS)
                     .build();
    }

    /**
     * Derives the entity variable QueryDSL names in generated queries: the Q-class name without its {@code Q}
     * prefix, decapitalized, or {@code entity} for a Q-class named otherwise.
     */
    private static String entityVariableName(ClassName qClass) {
        String name = qClass.simpleName();
        return name.startsWith("Q") && name.length() > 1
                ? Character.toLowerCase(name.charAt(1)) + name.substring(2)
                : "entity";
    }

    /**
     * Appends the statement and-ing a field's condition: null checks when their Boolean flag is true, and
     * value operators when the filter value is set. IN ignores null elements of the filter collection.
     */
    private void addFieldCondition(MethodSpec.Builder method, FieldMapping field) {
        CodeBlock getter = CodeBlock.of("dto.$N()", field.getterName());
        CodeBlock qPath = CodeBlock.of("q.$L", String.join(".", field.target().members()));
        boolean ignoreCase = field.ignoreCase();

        CodeBlock guard = switch (field.op()) {
            case IS_NULL, IS_NOT_NULL -> CodeBlock.of("$T.TRUE.equals($L)", Boolean.class, getter);
            default -> CodeBlock.of("$L != null", getter);
        };
        CodeBlock condition = switch (field.op()) {
            case EQ -> CodeBlock.of("$L.$N($L)", qPath, ignoreCase ? "equalsIgnoreCase" : "eq", getter);
            case NOT_EQ -> CodeBlock.of("$L.$N($L)", qPath, ignoreCase ? "notEqualsIgnoreCase" : "ne", getter);
            case LTE -> CodeBlock.of("$L.loe($L)", qPath, getter);
            case GTE -> CodeBlock.of("$L.goe($L)", qPath, getter);
            case LIKE -> CodeBlock.of("$L.$N($L, '$L')", qPath, ignoreCase ? "likeIgnoreCase" : "like", getter,
                                      LIKE_ESCAPE);
            case CONTAINS -> CodeBlock.of("$L.$N($L)", qPath, ignoreCase ? "containsIgnoreCase" : "contains",
                                          getter);
            case IN -> CodeBlock.of("$L.in($L.stream().filter($T::nonNull).toList())", qPath, getter,
                                    Objects.class);
            case IS_NULL -> CodeBlock.of("$L.isNull()", qPath);
            case IS_NOT_NULL -> CodeBlock.of("$L.isNotNull()", qPath);
            case REGEX -> throw new IllegalStateException("REGEX is rejected before code generation");
        };
        method.beginControlFlow("if ($L)", guard)
              .addStatement("builder.and($L)", condition)
              .endControlFlow();
    }
}
