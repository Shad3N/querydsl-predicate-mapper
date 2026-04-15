package io.github.shad3n.predicatemapper.codegen;

import com.google.testing.compile.Compilation;
import com.palantir.javapoet.ClassName;
import io.github.shad3n.predicatemapper.annotation.Op;
import io.github.shad3n.predicatemapper.testinfra.AbstractProcessorTest;
import io.github.shad3n.predicatemapper.testinfra.TestSourceFactory;

import static io.github.shad3n.predicatemapper.testinfra.GeneratedCodeAssert.assertThat;

public abstract class AbstractOperatorCodegenTest extends AbstractProcessorTest {

    protected static final ClassName Q_TEST_PRODUCT = ClassName.get(
            "io.github.shad3n.predicatemapper.testinfra.entity", "QTestProduct");

    protected void assertSingleFieldMapping(Class<?> fieldType, String fieldName, String qPath, Op op,
                                            String expectedMethod) throws Exception {
        String filterName = capitalize(fieldName) + "Filter";
        String mapperName = capitalize(fieldName) + "Mapper";

        var dto = TestSourceFactory.dto("test.dto", filterName)
                                   .field(fieldName, fieldType, qPath, op);

        var mapper = TestSourceFactory.mapper("test.mapper", mapperName)
                                      .method("filter", Q_TEST_PRODUCT, ClassName.get("test.dto", filterName));

        var setup = TestSourceFactory.setup().build(dto, mapper);

        // Compile generated sources
        Compilation compilation = compile(setup.sources().toArray(new javax.tools.JavaFileObject[0]));
        assertSuccessfulCompilation(compilation, "test.mapper." + mapperName + "Impl");

        String generatedSource = getGeneratedSource(compilation, "test.mapper." + mapperName + "Impl");

        // Assert structural correctness
        assertThat(generatedSource)
                .hasMethod("filter")
                .hasIfStatementCount("filter", 1)
                .hasNullCheck("filter", fieldName)
                .hasOperatorUsage("filter", qPath, expectedMethod);
    }

    protected String capitalize(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

