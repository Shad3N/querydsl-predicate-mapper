package io.github.shad3n.predicatemapper;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.JavaFileObjects;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.WildcardTypeName;
import io.github.shad3n.predicatemapper.annotation.Op;
import io.github.shad3n.predicatemapper.annotation.ToJavaPredicateMapper;
import io.github.shad3n.predicatemapper.testinfra.AbstractProcessorTest;
import io.github.shad3n.predicatemapper.testinfra.TestSourceFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import javax.tools.JavaFileObject;
import java.util.List;
import java.util.Set;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Java Predicate Backend Processor Tests")
class JavaPredicateBackendProcessorTest extends AbstractProcessorTest {

    private static final ClassName TEST_PRODUCT =
            ClassName.get("io.github.shad3n.predicatemapper.testinfra.entity", "TestProduct");
    private static final ClassName Q_TEST_PRODUCT =
            ClassName.get("io.github.shad3n.predicatemapper.testinfra.entity", "QTestProduct");
    private static final ClassName IMAGE = ClassName.get("test.target", "Image");
    private static final ClassName FILTER = ClassName.get("test.dto", "Filter");

    private static final JavaFileObject IMAGE_SOURCE = JavaFileObjects.forSourceString("test.target.Image", """
            package test.target;
            public record Image(String name, boolean custom, int size, Image parent) {}
            """);

    private static final ClassName CRATE = ClassName.get("test.target", "Crate");

    private static final JavaFileObject CRATE_SOURCE = JavaFileObjects.forSourceString("test.target.Crate", """
            package test.target;
            public record Crate(java.math.BigDecimal weight) {}
            """);

    private Compilation compileJavaMapper(ClassName target, TestSourceFactory.DtoBuilder dto) {
        var mapper = TestSourceFactory.mapper("test.mapper", "FilterMapper").javaMethod("matcher", target, FILTER);
        return compile(IMAGE_SOURCE, dto.build(), mapper.build());
    }

    private String generated(Compilation compilation) throws Exception {
        assertSuccessfulCompilation(compilation, "test.mapper.FilterMapperImpl");
        return getGeneratedSource(compilation, "test.mapper.FilterMapperImpl");
    }

    @Nested
    @DisplayName("Code generation")
    class CodeGeneration {

        @Test
        @DisplayName("Generates a JDK-only predicate for a record target")
        void generatesJdkOnlyPredicateForRecord() throws Exception {
            var dto = TestSourceFactory.dto("test.dto", "Filter")
                                       .field("name", String.class, "name", Op.EQ, true)
                                       .field("pattern", String.class, "name", Op.REGEX, true)
                                       .field("like", String.class, "name", Op.LIKE)
                                       .field("custom", Boolean.class, "custom", Op.EQ)
                                       .field("maxSize", Integer.class, "size", Op.LTE)
                                       .field("parentName", String.class, "parent.name", Op.EQ);

            String source = generated(compileJavaMapper(IMAGE, dto));

            assertThat(source).doesNotContain("querydsl")
                              .contains("Predicate<Image> matcher(Filter dto)")
                              .contains("equalsIgnoreCase")
                              .contains("regexPattern(pattern, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE, \"pattern\")")
                              .contains("likePattern(like, false)")
                              .contains("var member = target.parent()")
                              .contains("if (member == null)")
                              .doesNotContain("Optional.ofNullable");
        }

        @Test
        @DisplayName("Reads each intermediate of a nested path into a local and rejects a null one")
        void readsIntermediatesIntoLocals() throws Exception {
            var dto = TestSourceFactory.dto("test.dto", "Filter")
                                       .field("noParentName", Boolean.class, "parent.name", Op.IS_NULL);

            String source = generated(compileJavaMapper(IMAGE, dto));

            assertThat(source).contains("var member = target.parent()")
                              .contains("if (member == null)")
                              .contains("return false")
                              .contains("var value = member.name()")
                              .contains("return value == null");
        }

        @Test
        @DisplayName("EQ, NOT_EQ and IN on a Comparable path compare with compareTo")
        void comparablePathsUseCompareTo() throws Exception {
            var dto = TestSourceFactory.dto("test.dto", "Filter")
                                       .field("weight", java.math.BigDecimal.class, "weight", Op.EQ)
                                       .field("notWeight", java.math.BigDecimal.class, "weight", Op.NOT_EQ)
                                       .field("weights", ParameterizedTypeName.get(
                                               ClassName.get(List.class), ClassName.get(java.math.BigDecimal.class)),
                                              "weight", Op.IN);
            var mapper = TestSourceFactory.mapper("test.mapper", "FilterMapper").javaMethod("matcher", CRATE, FILTER);

            Compilation compilation = compile(CRATE_SOURCE, dto.build(), mapper.build());

            assertThat(generated(compilation)).contains("weight.compareTo(value) == 0")
                                              .contains("notWeight.compareTo(value) != 0")
                                              .contains("weights.stream().anyMatch(candidate -> candidate != null "
                                                                + "&& ((BigDecimal) candidate).compareTo(value) == 0)")
                                              .doesNotContain("weight.equals(value)");
        }

        @Test
        @DisplayName("ignoreCase EQ and NOT_EQ keep equalsIgnoreCase instead of compareTo")
        void ignoreCaseKeepsEqualsIgnoreCase() throws Exception {
            var dto = TestSourceFactory.dto("test.dto", "Filter")
                                       .field("name", String.class, "name", Op.EQ, true)
                                       .field("notName", String.class, "name", Op.NOT_EQ, true);

            String source = generated(compileJavaMapper(IMAGE, dto));

            assertThat(source).contains("name.equalsIgnoreCase(value)")
                              .contains("!notName.equalsIgnoreCase(value)")
                              .doesNotContain("compareTo");
        }

        @Test
        @DisplayName("Case-insensitive CONTAINS, LIKE and REGEX fold Unicode case")
        void ignoreCaseFoldsUnicode() throws Exception {
            var dto = TestSourceFactory.dto("test.dto", "Filter")
                                       .field("contains", String.class, "name", Op.CONTAINS, true)
                                       .field("regex", String.class, "name", Op.REGEX, true)
                                       .field("like", String.class, "name", Op.LIKE, true);

            String source = generated(compileJavaMapper(IMAGE, dto));

            assertThat(source).contains("Pattern.compile(Pattern.quote(contains), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)")
                              .contains("regexPattern(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE, \"regex\")")
                              .contains("likePattern(like, true)")
                              .contains("Pattern.DOTALL | (ignoreCase ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0)");
        }

        @Test
        @DisplayName("Generates a predicate for a getter-based target with nested path")
        void generatesPredicateForGetterTarget() throws Exception {
            var dto = TestSourceFactory.dto("test.dto", "Filter")
                                       .field("categoryName", String.class, "category.name", Op.EQ)
                                       .field("statuses", ParameterizedTypeName.get(
                                               ClassName.get(List.class),
                                               ClassName.get(
                                                       io.github.shad3n.predicatemapper.testinfra.entity.TestProduct.ProductStatus.class)),
                                              "status", Op.IN)
                                       .field("noDescription", Boolean.class, "description", Op.IS_NULL);

            String source = generated(compileJavaMapper(TEST_PRODUCT, dto));

            assertThat(source).contains("target.getCategory()").contains("member.getName()");
        }

        @Test
        @DisplayName("DTO fields named like generated locals still compile")
        void avoidsLocalNameCollisions() throws Exception {
            var dto = TestSourceFactory.dto("test.dto", "Filter")
                                       .field("target", String.class, "name", Op.EQ)
                                       .field("value", String.class, "name", Op.LIKE)
                                       .field("predicate", String.class, "name", Op.REGEX)
                                       .field("member", String.class, "parent.name", Op.EQ);

            generated(compileJavaMapper(IMAGE, dto));
        }

        @Test
        @DisplayName("One interface implements a QueryDSL and a Java method")
        void mixesBackendsInOneInterface() throws Exception {
            var dto = TestSourceFactory.dto("test.dto", "Filter").field("name", String.class, "name", Op.EQ);
            var mapper = TestSourceFactory.mapper("test.mapper", "FilterMapper")
                                          .method("query", Q_TEST_PRODUCT, FILTER)
                                          .javaMethod("matcher", TEST_PRODUCT, FILTER);

            Compilation compilation = compile(dto.build(), mapper.build());

            assertSuccessfulCompilation(compilation, "test.mapper.FilterMapperImpl");
            String source = getGeneratedSource(compilation, "test.mapper.FilterMapperImpl");
            assertThat(source).contains("BooleanBuilder").contains("Predicate<TestProduct> matcher(Filter dto)");
        }

        @Test
        @DisplayName("Fields inherited from a DTO superclass are mapped on both backends")
        void mapsInheritedFields() throws Exception {
            JavaFileObject base = JavaFileObjects.forSourceString("test.dto.BaseFilter", """
                    package test.dto;
                    import io.github.shad3n.predicatemapper.annotation.FilterField;
                    import io.github.shad3n.predicatemapper.annotation.Op;
                    public class BaseFilter {
                        @FilterField(path = "name", op = Op.EQ)
                        private String name;
                        public String getName() { return name; }
                    }
                    """);
            JavaFileObject filter = JavaFileObjects.forSourceString("test.dto.Filter", """
                    package test.dto;
                    import io.github.shad3n.predicatemapper.annotation.FilterField;
                    import io.github.shad3n.predicatemapper.annotation.Op;
                    public class Filter extends BaseFilter {
                        @FilterField(path = "stock", op = Op.GTE)
                        private Integer minStock;
                        public Integer getMinStock() { return minStock; }
                    }
                    """);
            var mapper = TestSourceFactory.mapper("test.mapper", "FilterMapper")
                                          .method("query", Q_TEST_PRODUCT, FILTER)
                                          .javaMethod("matcher", TEST_PRODUCT, FILTER);

            Compilation compilation = compile(base, filter, mapper.build());

            assertSuccessfulCompilation(compilation, "test.mapper.FilterMapperImpl");
            assertThat(getGeneratedSource(compilation, "test.mapper.FilterMapperImpl"))
                    .contains("q.name.eq(dto.getName())")
                    .contains("var name = dto.getName()")
                    .contains("var minStock = dto.getMinStock()");
        }

        @Test
        @DisplayName("QueryDSL ignoreCase uses the case-insensitive path methods")
        void queryDslIgnoreCase() throws Exception {
            var dto = TestSourceFactory.dto("test.dto", "Filter")
                                       .field("name", String.class, "name", Op.EQ, true)
                                       .field("notName", String.class, "name", Op.NOT_EQ, true)
                                       .field("nameLike", String.class, "name", Op.LIKE, true)
                                       .field("nameContains", String.class, "name", Op.CONTAINS, true);
            var mapper = TestSourceFactory.mapper("test.mapper", "FilterMapper")
                                          .method("query", Q_TEST_PRODUCT, FILTER);

            Compilation compilation = compile(dto.build(), mapper.build());

            assertSuccessfulCompilation(compilation, "test.mapper.FilterMapperImpl");
            assertThat(getGeneratedSource(compilation, "test.mapper.FilterMapperImpl"))
                    .contains("q.name.equalsIgnoreCase(dto.getName())")
                    .contains("q.name.notEqualsIgnoreCase(dto.getNotName())")
                    .contains("q.name.likeIgnoreCase(dto.getNameLike(), '!')")
                    .contains("q.name.containsIgnoreCase(dto.getNameContains())");
        }

        @Test
        @DisplayName("QueryDSL LIKE passes the escape character")
        void queryDslLikeEscape() throws Exception {
            var dto = TestSourceFactory.dto("test.dto", "Filter")
                                       .field("nameLike", String.class, "name", Op.LIKE);
            var mapper = TestSourceFactory.mapper("test.mapper", "FilterMapper")
                                          .method("query", Q_TEST_PRODUCT, FILTER);

            Compilation compilation = compile(dto.build(), mapper.build());

            assertSuccessfulCompilation(compilation, "test.mapper.FilterMapperImpl");
            assertThat(getGeneratedSource(compilation, "test.mapper.FilterMapperImpl"))
                    .contains("q.name.like(dto.getNameLike(), '!')");
        }

        @Test
        @DisplayName("Offset date-time paths compare by instant and Double paths by numeric value")
        void instantAndFloatingPointComparisons() throws Exception {
            JavaFileObject reading = JavaFileObjects.forSourceString("test.target.Reading", """
                    package test.target;
                    public record Reading(java.time.OffsetDateTime takenAt, Double load) {}
                    """);
            var dto = TestSourceFactory.dto("test.dto", "Filter")
                                       .field("latest", java.time.OffsetDateTime.class, "takenAt", Op.LTE)
                                       .field("earliest", java.time.OffsetDateTime.class, "takenAt", Op.GTE)
                                       .field("load", Double.class, "load", Op.EQ)
                                       .field("notLoad", Double.class, "load", Op.NOT_EQ)
                                       .field("maxLoad", Double.class, "load", Op.LTE)
                                       .field("minLoad", Double.class, "load", Op.GTE)
                                       .field("loads", ParameterizedTypeName.get(ClassName.get(List.class),
                                                                                 ClassName.get(Double.class)),
                                              "load", Op.IN);
            var mapper = TestSourceFactory.mapper("test.mapper", "FilterMapper")
                                          .javaMethod("matcher", ClassName.get("test.target", "Reading"), FILTER);

            String source = generated(compile(reading, dto.build(), mapper.build()));

            assertThat(source).contains("!latest.isBefore(value)")
                              .contains("!earliest.isAfter(value)")
                              .contains("load.doubleValue() == value")
                              .contains("notLoad.doubleValue() != value")
                              .contains("maxLoad.doubleValue() >= value")
                              .contains("minLoad.doubleValue() <= value")
                              .contains("((Double) candidate).doubleValue() == value");
        }

        @Test
        @DisplayName("QueryDSL IN drops null elements before building the predicate")
        void queryDslInDropsNullElements() throws Exception {
            var dto = TestSourceFactory.dto("test.dto", "Filter")
                                       .field("names", ParameterizedTypeName.get(ClassName.get(List.class),
                                                                                 ClassName.get(String.class)),
                                              "name", Op.IN);
            var mapper = TestSourceFactory.mapper("test.mapper", "FilterMapper")
                                          .method("query", Q_TEST_PRODUCT, FILTER);

            Compilation compilation = compile(dto.build(), mapper.build());

            assertSuccessfulCompilation(compilation, "test.mapper.FilterMapperImpl");
            assertThat(getGeneratedSource(compilation, "test.mapper.FilterMapperImpl"))
                    .contains("q.name.in(dto.getNames().stream().filter(Objects::nonNull).toList())");
        }
    }

    @Nested
    @DisplayName("Compile errors")
    class CompileErrors {

        private void assertError(Compilation compilation, String message) {
            assertFailedCompilation(compilation);
            assertThat(compilation).hadErrorContaining(message);
        }

        @Test
        @DisplayName("REGEX on a QueryDSL method fails on the mapper annotation")
        void regexOnQueryDsl() {
            var dto = TestSourceFactory.dto("test.dto", "Filter").field("name", String.class, "name", Op.REGEX);
            var mapper = TestSourceFactory.mapper("test.mapper", "FilterMapper").method("query", Q_TEST_PRODUCT, FILTER);

            assertError(compile(dto.build(), mapper.build()), "cannot map Op.REGEX");
        }

        @Test
        @DisplayName("Return type must match the Java target")
        void wrongReturnType() {
            var dto = TestSourceFactory.dto("test.dto", "Filter").field("name", String.class, "name", Op.EQ);
            var mapper = TestSourceFactory.mapper("test.mapper", "FilterMapper")
                                          .annotatedMethod("matcher", ToJavaPredicateMapper.class, IMAGE,
                                                           ParameterizedTypeName.get(
                                                                   ClassName.get(java.util.function.Predicate.class),
                                                                   ClassName.OBJECT),
                                                           FILTER);

            assertError(compile(IMAGE_SOURCE, dto.build(), mapper.build()),
                        "must return java.util.function.Predicate<test.target.Image>");
        }

        @Test
        @DisplayName("Both mapper annotations on one method fail")
        void conflictingAnnotations() {
            JavaFileObject mapper = JavaFileObjects.forSourceString("test.mapper.FilterMapper", """
                    package test.mapper;
                    @io.github.shad3n.predicatemapper.annotation.PredicateMapper
                    public interface FilterMapper {
                        @io.github.shad3n.predicatemapper.annotation.ToJavaPredicateMapper(test.target.Image.class)
                        @io.github.shad3n.predicatemapper.annotation.ToQueryDslPredicateMapper(test.target.Image.class)
                        java.util.function.Predicate<test.target.Image> matcher(test.dto.Filter dto);
                    }
                    """);
            var dto = TestSourceFactory.dto("test.dto", "Filter").field("name", String.class, "name", Op.EQ);

            assertError(compile(IMAGE_SOURCE, dto.build(), mapper), "more than one mapper annotation");
        }

        @ParameterizedTest
        @EnumSource(value = Op.class, names = {"LTE", "GTE", "IN", "IS_NULL", "IS_NOT_NULL"})
        @DisplayName("ignoreCase on a non-text operator fails")
        void ignoreCaseOnNonTextOperator(Op op) {
            var dto = TestSourceFactory.dto("test.dto", "Filter").field("name", String.class, "name", op, true);

            assertError(compileJavaMapper(IMAGE, dto), "ignoreCase applies to EQ, NOT_EQ, LIKE, CONTAINS and REGEX only");
        }

        @Test
        @DisplayName("Primitive DTO field fails on both backends")
        void primitiveDtoField() {
            var dto = TestSourceFactory.dto("test.dto", "Filter")
                                       .field("maxSize", com.palantir.javapoet.TypeName.INT, "size", Op.LTE);
            assertError(compileJavaMapper(IMAGE, dto), "returns the primitive int");

            var queryDslDto = TestSourceFactory.dto("test.dto", "Filter")
                                               .field("minStock", com.palantir.javapoet.TypeName.INT, "stock", Op.GTE);
            var mapper = TestSourceFactory.mapper("test.mapper", "FilterMapper").method("query", Q_TEST_PRODUCT, FILTER);
            assertError(compile(queryDslDto.build(), mapper.build()), "DTO field is int");
        }

        @Test
        @DisplayName("Boxed DTO field read through a primitive getter fails")
        void boxedFieldWithPrimitiveGetter() {
            JavaFileObject dto = JavaFileObjects.forSourceString("test.dto.Filter", """
                    package test.dto;
                    import io.github.shad3n.predicatemapper.annotation.FilterField;
                    import io.github.shad3n.predicatemapper.annotation.Op;
                    public class Filter {
                        @FilterField(path = "size", op = Op.LTE)
                        private Integer maxSize;
                        public int getMaxSize() { return maxSize; }
                    }
                    """);
            var mapper = TestSourceFactory.mapper("test.mapper", "FilterMapper").javaMethod("matcher", IMAGE, FILTER);

            assertError(compile(IMAGE_SOURCE, dto, mapper.build()), "getMaxSize() returns the primitive int");
        }

        @Test
        @DisplayName("Abstract mapper method without a mapper annotation fails")
        void missingMapperAnnotation() {
            JavaFileObject mapper = JavaFileObjects.forSourceString("test.mapper.FilterMapper", """
                    package test.mapper;
                    @io.github.shad3n.predicatemapper.annotation.PredicateMapper
                    public interface FilterMapper {
                        java.util.function.Predicate<test.target.Image> matcher(test.dto.Filter dto);
                    }
                    """);
            var dto = TestSourceFactory.dto("test.dto", "Filter").field("name", String.class, "name", Op.EQ);

            assertError(compile(IMAGE_SOURCE, dto.build(), mapper), "needs @ToQueryDslPredicateMapper or");
        }

        @Test
        @DisplayName("ignoreCase on a non-String path fails on both backends")
        void ignoreCaseOnNonStringPath() {
            var dto = TestSourceFactory.dto("test.dto", "Filter").field("size", Integer.class, "size", Op.EQ, true);
            assertError(compileJavaMapper(IMAGE, dto), "ignoreCase needs a String path");

            var queryDslDto = TestSourceFactory.dto("test.dto", "Filter")
                                               .field("stock", Integer.class, "stock", Op.EQ, true);
            var mapper = TestSourceFactory.mapper("test.mapper", "FilterMapper").method("query", Q_TEST_PRODUCT, FILTER);
            assertError(compile(queryDslDto.build(), mapper.build()), "ignoreCase needs a String path");
        }

        @Test
        @DisplayName("CONTAINS on a non-String path fails on both backends")
        void containsOnNonStringPath() {
            var dto = TestSourceFactory.dto("test.dto", "Filter").field("size", Integer.class, "size", Op.CONTAINS);
            assertError(compileJavaMapper(IMAGE, dto), "path maps to java.lang.String but DTO field is");

            var queryDslDto = TestSourceFactory.dto("test.dto", "Filter")
                                               .field("stock", Integer.class, "stock", Op.CONTAINS);
            var mapper = TestSourceFactory.mapper("test.mapper", "FilterMapper").method("query", Q_TEST_PRODUCT, FILTER);
            assertError(compile(queryDslDto.build(), mapper.build()), "CONTAINS needs a String path");
        }

        @Test
        @DisplayName("CONTAINS with a non-String DTO field fails on both backends")
        void containsWithNonStringField() {
            var dto = TestSourceFactory.dto("test.dto", "Filter").field("name", Integer.class, "name", Op.CONTAINS);
            assertError(compileJavaMapper(IMAGE, dto), "DTO field is java.lang.Integer");

            var queryDslDto = TestSourceFactory.dto("test.dto", "Filter")
                                               .field("name", Integer.class, "name", Op.CONTAINS);
            var mapper = TestSourceFactory.mapper("test.mapper", "FilterMapper").method("query", Q_TEST_PRODUCT, FILTER);
            assertError(compile(queryDslDto.build(), mapper.build()), "DTO field is java.lang.Integer");
        }

        @Test
        @DisplayName("Null check on a primitive accessor fails")
        void nullCheckOnPrimitive() {
            var dto = TestSourceFactory.dto("test.dto", "Filter").field("noSize", Boolean.class, "size", Op.IS_NULL);

            assertError(compileJavaMapper(IMAGE, dto), "which is never null");
        }

        @ParameterizedTest
        @EnumSource(value = Op.class, names = {"IS_NULL", "IS_NOT_NULL"})
        @DisplayName("Null check on a primitive leaf fails at any path depth")
        void nullCheckOnNestedPrimitive(Op op) {
            var dto = TestSourceFactory.dto("test.dto", "Filter").field("parentSize", Boolean.class, "parent.size", op);

            assertError(compileJavaMapper(IMAGE, dto), "which is never null");
        }

        @Test
        @DisplayName("Unknown accessor fails")
        void unknownAccessor() {
            var dto = TestSourceFactory.dto("test.dto", "Filter").field("missing", String.class, "missing", Op.EQ);

            assertError(compileJavaMapper(IMAGE, dto), "no public accessor 'missing()'");
        }

        @Test
        @DisplayName("Filter type differing from the path type fails")
        void typeMismatch() {
            var dto = TestSourceFactory.dto("test.dto", "Filter").field("name", Integer.class, "name", Op.EQ);

            assertError(compileJavaMapper(IMAGE, dto), "Type mismatch");
        }

        @Test
        @DisplayName("IN needs a collection of the path type")
        void inWithWrongElementType() {
            var dto = TestSourceFactory.dto("test.dto", "Filter")
                                       .field("names", ParameterizedTypeName.get(ClassName.get(List.class),
                                                                                 ClassName.get(Integer.class)),
                                              "name", Op.IN);

            assertError(compileJavaMapper(IMAGE, dto), "Type mismatch");
        }

        @Test
        @DisplayName("IN with a raw collection fails")
        void inWithRawCollection() {
            var dto = TestSourceFactory.dto("test.dto", "Filter").field("names", List.class, "name", Op.IN);

            assertError(compileJavaMapper(IMAGE, dto), "Type mismatch");
        }

        @Test
        @DisplayName("IN with a collection subclass fixing another element type fails")
        void inWithCollectionSubclassOfWrongElementType() {
            JavaFileObject names = JavaFileObjects.forSourceString("test.dto.Names", """
                    package test.dto;
                    public class Names extends java.util.ArrayList<String> {}
                    """);
            JavaFileObject filter = inFilterSource("Names", "size");
            var mapper = TestSourceFactory.mapper("test.mapper", "FilterMapper").javaMethod("matcher", IMAGE, FILTER);

            assertError(compile(IMAGE_SOURCE, names, filter, mapper.build()), "Type mismatch");
        }

        @Test
        @DisplayName("IN with a collection subclass fixing the path type compiles")
        void inWithCollectionSubclassOfPathType() throws Exception {
            JavaFileObject names = JavaFileObjects.forSourceString("test.dto.Names", """
                    package test.dto;
                    public class Names extends java.util.ArrayList<String> {}
                    """);
            JavaFileObject filter = inFilterSource("Names", "name");
            var mapper = TestSourceFactory.mapper("test.mapper", "FilterMapper").javaMethod("matcher", IMAGE, FILTER);

            generated(compile(IMAGE_SOURCE, names, filter, mapper.build()));
        }

        @Test
        @DisplayName("IN with a wildcard-bounded collection of the path type compiles")
        void inWithUpperBoundedWildcard() throws Exception {
            var dto = TestSourceFactory.dto("test.dto", "Filter")
                                       .field("sizes", ParameterizedTypeName.get(
                                               ClassName.get(Set.class), WildcardTypeName.subtypeOf(Integer.class)),
                                              "size", Op.IN);

            generated(compileJavaMapper(IMAGE, dto));
        }

        private JavaFileObject inFilterSource(String collectionType, String path) {
            return JavaFileObjects.forSourceString("test.dto.Filter", """
                    package test.dto;
                    import io.github.shad3n.predicatemapper.annotation.FilterField;
                    import io.github.shad3n.predicatemapper.annotation.Op;
                    public class Filter {
                        @FilterField(path = "%s", op = Op.IN)
                        private %s values;
                        public %s getValues() { return values; }
                    }
                    """.formatted(path, collectionType, collectionType));
        }

        @Test
        @DisplayName("Removed @ToPredicate fails compilation")
        void removedToPredicate() {
            JavaFileObject mapper = JavaFileObjects.forSourceString("test.mapper.FilterMapper", """
                    package test.mapper;
                    @io.github.shad3n.predicatemapper.annotation.PredicateMapper
                    public interface FilterMapper {
                        @io.github.shad3n.predicatemapper.annotation.ToPredicate(test.target.Image.class)
                        java.util.function.Predicate<test.target.Image> matcher(test.dto.Filter dto);
                    }
                    """);
            var dto = TestSourceFactory.dto("test.dto", "Filter").field("name", String.class, "name", Op.EQ);

            assertError(compile(IMAGE_SOURCE, dto.build(), mapper), "ToPredicate");
        }
    }
}
