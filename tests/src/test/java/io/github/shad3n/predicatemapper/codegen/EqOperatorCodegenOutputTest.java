package io.github.shad3n.predicatemapper.codegen;

import com.google.testing.compile.Compilation;
import com.palantir.javapoet.ClassName;
import io.github.shad3n.predicatemapper.annotation.Op;
import io.github.shad3n.predicatemapper.testinfra.TestSourceFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static io.github.shad3n.predicatemapper.testinfra.GeneratedCodeAssert.assertThat;

/**
 * Tests for the EQ (equals) operator code generation.
 * Verifies that the annotation processor correctly generates predicate logic
 * for equality comparisons across various field types.
 * <p>
 * Uses TestProduct entity fields: name, description, price, stock, version,
 * weight, active, status, createdDate, lastModified, openTime, category
 */
@DisplayName("Operator Codegen Tests")
class EqOperatorCodegenOutputTest extends AbstractOperatorCodegenTest {

    @Test
    @DisplayName("Should generate eq() call for String field")
    void shouldGenerateEqCallForStringField() throws Exception {
        assertSingleFieldMapping(String.class, "name", "name", Op.EQ, "eq");
    }

    @Test
    @DisplayName("Should generate eq() call for Integer field")
    void shouldGenerateEqCallForIntegerField() throws Exception {
        assertSingleFieldMapping(Integer.class, "stock", "stock", Op.EQ, "eq");
    }

    @Test
    @DisplayName("Should generate eq() call for Boolean field")
    void shouldGenerateEqCallForBooleanField() throws Exception {
        assertSingleFieldMapping(Boolean.class, "active", "active", Op.EQ, "eq");
    }

    @Test
    @DisplayName("Should generate eq() call for Long field")
    void shouldGenerateEqCallForLongField() throws Exception {
        assertSingleFieldMapping(Long.class, "version", "version", Op.EQ, "eq");
    }

    @Test
    @DisplayName("Should generate eq() call for Double field")
    void shouldGenerateEqCallForDoubleField() throws Exception {
        assertSingleFieldMapping(Double.class, "weight", "weight", Op.EQ, "eq");
    }

    @Test
    @DisplayName("Should generate eq() call for BigDecimal field")
    void shouldGenerateEqCallForBigDecimalField() throws Exception {
        assertSingleFieldMapping(BigDecimal.class, "price", "price", Op.EQ, "eq");
    }

    @Test
    @DisplayName("Should generate multiple eq() calls for multiple fields")
    void shouldGenerateMultipleEqCalls() throws Exception {
        var dto = TestSourceFactory.dto("test.dto", "MultiFilter")
                                   .field("name", String.class, "name", Op.EQ)
                                   .field("stock", Integer.class, "stock", Op.EQ)
                                   .field("active", Boolean.class, "active", Op.EQ);

        var mapper = TestSourceFactory.mapper("test.mapper", "MultiMapper")
                                      .method("filter", AbstractOperatorCodegenTest.Q_TEST_PRODUCT,
                                              ClassName.get("test.dto", "MultiFilter"));

        var setup = TestSourceFactory.setup()
                                     .build(dto, mapper);

        Compilation compilation = compile(setup.sources().toArray(new javax.tools.JavaFileObject[0]));
        assertSuccessfulCompilation(compilation, "test.mapper.MultiMapperImpl");

        String generatedSource = getGeneratedSource(compilation, "test.mapper.MultiMapperImpl");

        assertThat(generatedSource)
                .hasMethod("filter")
                .hasIfStatementCount("filter", 3)
                .hasNullCheck("filter", "name")
                .hasNullCheck("filter", "stock")
                .hasNullCheck("filter", "active")
                .hasOperatorUsage("filter", "name", "eq")
                .hasOperatorUsage("filter", "stock", "eq")
                .hasOperatorUsage("filter", "active", "eq");
    }

    @Test
    @DisplayName("Should generate correct predicate structure with BooleanBuilder")
    void shouldGenerateCorrectPredicateStructure() throws Exception {
        var dto = TestSourceFactory.dto("test.dto", "StructFilter")
                                   .field("name", String.class, "name", Op.EQ)
                                   .field("active", Boolean.class, "active", Op.EQ);

        var mapper = TestSourceFactory.mapper("test.mapper", "StructMapper")
                                      .method("filter", AbstractOperatorCodegenTest.Q_TEST_PRODUCT,
                                              ClassName.get("test.dto", "StructFilter"));

        var setup = TestSourceFactory.setup()
                                     .build(dto, mapper);

        Compilation compilation = compile(setup.sources().toArray(new javax.tools.JavaFileObject[0]));
        assertSuccessfulCompilation(compilation, "test.mapper.StructMapperImpl");

        String generatedSource = getGeneratedSource(compilation, "test.mapper.StructMapperImpl");

        assertThat(generatedSource)
                .hasMethod("filter")
                .createsQClass("filter", "QTestProduct")
                .createsBooleanBuilder("filter")
                .returnsPredicate("filter");
    }

    @Test
    @DisplayName("Should generate eq() call for nested path")
    void shouldGenerateEqCallForNestedPath() throws Exception {
        var dto = TestSourceFactory.dto("test.dto", "CategoryNameFilter")
                                   .field("categoryName", String.class, "category.name", Op.EQ);

        var mapper = TestSourceFactory.mapper("test.mapper", "CategoryNameMapper")
                                      .method("filter", AbstractOperatorCodegenTest.Q_TEST_PRODUCT,
                                              ClassName.get("test.dto", "CategoryNameFilter"));

        var setup = TestSourceFactory.setup()
                                     .build(dto, mapper);

        Compilation compilation = compile(setup.sources().toArray(new javax.tools.JavaFileObject[0]));
        assertSuccessfulCompilation(compilation, "test.mapper.CategoryNameMapperImpl");

        String generatedSource = getGeneratedSource(compilation, "test.mapper.CategoryNameMapperImpl");

        assertThat(generatedSource)
                .hasMethod("filter")
                .hasIfStatementCount("filter", 1)
                .hasNullCheck("filter", "categoryName")
                .methodContainsStatement("filter", "category.name");
    }

    @Test
    @DisplayName("Should generate eq() call for nested Boolean path")
    void shouldGenerateEqCallForNestedBooleanPath() throws Exception {
        var dto = TestSourceFactory.dto("test.dto", "CategoryActiveFilter")
                                   .field("categoryActive", Boolean.class, "category.active", Op.EQ);

        var mapper = TestSourceFactory.mapper("test.mapper", "CategoryActiveMapper")
                                      .method("filter", AbstractOperatorCodegenTest.Q_TEST_PRODUCT,
                                              ClassName.get("test.dto", "CategoryActiveFilter"));

        var setup = TestSourceFactory.setup()
                                     .build(dto, mapper);

        Compilation compilation = compile(setup.sources().toArray(new javax.tools.JavaFileObject[0]));
        assertSuccessfulCompilation(compilation, "test.mapper.CategoryActiveMapperImpl");

        String generatedSource = getGeneratedSource(compilation, "test.mapper.CategoryActiveMapperImpl");

        assertThat(generatedSource)
                .hasMethod("filter")
                .hasIfStatementCount("filter", 1)
                .hasNullCheck("filter", "categoryActive")
                .methodContainsStatement("filter", "category.active");
    }

    @Test
    @DisplayName("Should generate eq() calls for mixed direct and nested paths")
    void shouldGenerateEqCallsForMixedPaths() throws Exception {
        var dto = TestSourceFactory.dto("test.dto", "MixedFilter")
                                   .field("name", String.class, "name", Op.EQ)
                                   .field("categoryActive", Boolean.class, "category.active", Op.EQ)
                                   .field("stock", Integer.class, "stock", Op.EQ);

        var mapper = TestSourceFactory.mapper("test.mapper", "MixedMapper")
                                      .method("filter", AbstractOperatorCodegenTest.Q_TEST_PRODUCT,
                                              ClassName.get("test.dto", "MixedFilter"));

        var setup = TestSourceFactory.setup()
                                     .build(dto, mapper);

        Compilation compilation = compile(setup.sources().toArray(new javax.tools.JavaFileObject[0]));
        assertSuccessfulCompilation(compilation, "test.mapper.MixedMapperImpl");

        String generatedSource = getGeneratedSource(compilation, "test.mapper.MixedMapperImpl");

        assertThat(generatedSource)
                .hasMethod("filter")
                .hasIfStatementCount("filter", 3)
                .hasOperatorUsage("filter", "name", "eq")
                .methodContainsStatement("filter", "category.active")
                .hasOperatorUsage("filter", "stock", "eq");
    }

    @Test
    @DisplayName("Should implement the mapper interface")
    void shouldImplementMapperInterface() throws Exception {
        var dto = TestSourceFactory.dto("test.dto", "ImplFilter")
                                   .field("name", String.class, "name", Op.EQ);

        var mapper = TestSourceFactory.mapper("test.mapper", "ImplMapper")
                                      .method("filter", AbstractOperatorCodegenTest.Q_TEST_PRODUCT,
                                              ClassName.get("test.dto", "ImplFilter"));

        var setup = TestSourceFactory.setup()
                                     .build(dto, mapper);

        Compilation compilation = compile(setup.sources().toArray(new javax.tools.JavaFileObject[0]));
        assertSuccessfulCompilation(compilation, "test.mapper.ImplMapperImpl");

        String generatedSource = getGeneratedSource(compilation, "test.mapper.ImplMapperImpl");

        assertThat(generatedSource)
                .implementsInterface("ImplMapper")
                .hasMethodsExactly("filter");
    }

    @Test
    @DisplayName("Should generate correct null check pattern")
    void shouldGenerateCorrectNullCheckPattern() throws Exception {
        var dto = TestSourceFactory.dto("test.dto", "NullCheckFilter")
                                   .field("name", String.class, "name", Op.EQ);

        var mapper = TestSourceFactory.mapper("test.mapper", "NullCheckMapper")
                                      .method("filter", AbstractOperatorCodegenTest.Q_TEST_PRODUCT,
                                              ClassName.get("test.dto", "NullCheckFilter"));

        var setup = TestSourceFactory.setup()
                                     .build(dto, mapper);

        Compilation compilation = compile(setup.sources().toArray(new javax.tools.JavaFileObject[0]));
        assertSuccessfulCompilation(compilation, "test.mapper.NullCheckMapperImpl");

        String generatedSource = getGeneratedSource(compilation, "test.mapper.NullCheckMapperImpl");

        assertThat(generatedSource)
                .hasMethod("filter")
                .methodContainsCondition("filter", "getName() != null");
    }

    @Test
    @DisplayName("Should use builder.and() for combining predicates")
    void shouldUseBuilderAndForCombining() throws Exception {
        var dto = TestSourceFactory.dto("test.dto", "AndFilter")
                                   .field("name", String.class, "name", Op.EQ)
                                   .field("active", Boolean.class, "active", Op.EQ);

        var mapper = TestSourceFactory.mapper("test.mapper", "AndMapper")
                                      .method("filter", AbstractOperatorCodegenTest.Q_TEST_PRODUCT,
                                              ClassName.get("test.dto", "AndFilter"));

        var setup = TestSourceFactory.setup()
                                     .build(dto, mapper);

        Compilation compilation = compile(setup.sources().toArray(new javax.tools.JavaFileObject[0]));
        assertSuccessfulCompilation(compilation, "test.mapper.AndMapperImpl");

        String generatedSource = getGeneratedSource(compilation, "test.mapper.AndMapperImpl");

        assertThat(generatedSource)
                .hasMethod("filter")
                .methodContainsStatement("filter", "builder.and(");
    }
}
