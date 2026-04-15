package io.github.shad3n.predicatemapper;

import com.google.testing.compile.Compilation;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import io.github.shad3n.predicatemapper.annotation.Op;
import io.github.shad3n.predicatemapper.testinfra.AbstractProcessorTest;
import io.github.shad3n.predicatemapper.testinfra.TestSourceFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.shad3n.predicatemapper.testinfra.GeneratedCodeAssert.assertThat;

/**
 * Tests for the main annotation processor functionality.
 * <p>
 * Sanity check test before all other tests
 */
@DisplayName("Annotation Processor Tests")
@Order(1)
class FilterToQueryDslAnnotationProcessorTest extends AbstractProcessorTest {

    @Nested
    @DisplayName("Basic Code Generation")
    class BasicCodeGeneration {

        @Test
        @DisplayName("Should generate implementation for simple mapper")
        void shouldGenerateImplementation() throws Exception {
            var dto = TestSourceFactory.dto("test.dto", "UserFilter")
                                       .field("name", String.class, "name", Op.EQ)
                                       .field("description", String.class, "description", Op.EQ)
                                       .field("active", Boolean.class, "active", Op.EQ);

            var mapper = TestSourceFactory.mapper("test.mapper", "UserMapper")
                                          .method("filter",
                                                  ClassName.get("io.github.shad3n.predicatemapper.testinfra.entity",
                                                                "QTestProduct"),
                                                  ClassName.get("test.dto", "UserFilter"));

            var setup = TestSourceFactory.setup()
                                         .build(dto, mapper);

            Compilation compilation = compile(setup.sources().toArray(new javax.tools.JavaFileObject[0]));
            assertSuccessfulCompilation(compilation, "test.mapper.UserMapperImpl");
        }

        @Test
        @DisplayName("Should generate implementation with multiple fields")
        void shouldGenerateImplementationWithMultipleFields() throws Exception {
            var dto = TestSourceFactory.dto("test.dto", "ProductFilter")
                                       .field("stock", Integer.class, "stock", Op.EQ)
                                       .field("minStock", Integer.class, "stock", Op.GTE)
                                       .field("maxStock", Integer.class, "stock", Op.LTE)
                                       .field("price", java.math.BigDecimal.class, "price", Op.EQ)
                                       .field("namePattern", String.class, "name", Op.LIKE)
                                       .field("statuses", ParameterizedTypeName.get(ClassName.get(List.class),
                                                                                    ClassName.get(
                                                                                            io.github.shad3n.predicatemapper.testinfra.entity.TestProduct.ProductStatus.class)),
                                              "status", Op.IN);

            var mapper = TestSourceFactory.mapper("test.mapper", "ProductMapper")
                                          .method("filter",
                                                  ClassName.get("io.github.shad3n.predicatemapper.testinfra.entity",
                                                                "QTestProduct"),
                                                  ClassName.get("test.dto", "ProductFilter"));

            var setup = TestSourceFactory.setup()
                                         .build(dto, mapper);

            Compilation compilation = compile(setup.sources().toArray(new javax.tools.JavaFileObject[0]));
            assertSuccessfulCompilation(compilation, "test.mapper.ProductMapperImpl");

            String generatedSource = getGeneratedSource(compilation, "test.mapper.ProductMapperImpl");
            assertThat(generatedSource)
                    .hasMethod("filter")
                    .hasIfStatementCount("filter", 6);
        }
    }

    @Nested
    @DisplayName("Spring Integration")
    class SpringIntegration {

        @Test
        @DisplayName("Should add @Component when Spring is on classpath")
        void shouldAddComponentAnnotation() throws Exception {
            var dto = TestSourceFactory.dto("test.dto", "UserFilter")
                                       .field("name", String.class, "name", Op.EQ);

            var mapper = TestSourceFactory.mapper("test.mapper", "UserMapper")
                                          .method("filter",
                                                  ClassName.get("io.github.shad3n.predicatemapper.testinfra.entity",
                                                                "QTestProduct"),
                                                  ClassName.get("test.dto", "UserFilter"));

            var setup = TestSourceFactory.setup()
                                         .withSpring()
                                         .build(dto, mapper);

            Compilation compilation = compile(setup.sources().toArray(new javax.tools.JavaFileObject[0]));
            assertSuccessfulCompilation(compilation, "test.mapper.UserMapperImpl");

            String generatedSource = getGeneratedSource(compilation, "test.mapper.UserMapperImpl");
            assertThat(generatedSource).hasAnnotation("Component");
        }

    }
}
