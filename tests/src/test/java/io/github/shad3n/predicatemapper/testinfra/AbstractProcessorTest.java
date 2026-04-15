package io.github.shad3n.predicatemapper.testinfra;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import io.github.shad3n.predicatemapper.FilterToQueryDslAnnotationProcessor;

import javax.tools.JavaFileObject;

import static com.google.testing.compile.CompilationSubject.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Base class for annotation processor tests.
 * Uses isolated compilation - all dependencies must be provided as source files.
 */
public abstract class AbstractProcessorTest {

    /**
     * Compiles the given sources with the annotation processor.
     */
    protected Compilation compile(JavaFileObject... sources) {
        return Compiler.javac()
                       .withProcessors(new FilterToQueryDslAnnotationProcessor())
                       .compile(sources);
    }

    /**
     * Asserts that compilation succeeded and generated the expected implementation file.
     * Prints compilation errors if it fails.
     */
    protected void assertSuccessfulCompilation(Compilation compilation, String expectedImplName) {
        if (!compilation.errors().isEmpty()) {
            StringBuilder sb = new StringBuilder("Compilation failed with errors:\n");
            compilation.errors().forEach(e -> sb.append("  ").append(e.getMessage(null)).append("\n"));
            fail(sb.toString());
        }
        assertThat(compilation).succeeded();
        assertThat(compilation).generatedSourceFile(expectedImplName);
    }

    /**
     * Asserts that compilation failed with errors.
     */
    protected void assertFailedCompilation(Compilation compilation) {
        assertThat(compilation).failed();
    }

    /**
     * Retrieves the generated source file content.
     */
    protected String getGeneratedSource(Compilation compilation, String className) throws Exception {
        return compilation.generatedSourceFile(className)
                          .orElseThrow(() -> new AssertionError("Expected generated file: " + className +
                                                                        ". Generated files: " +
                                                                        compilation.generatedSourceFiles().stream()
                                                                                   .map(f -> f.getName())
                                                                                   .toList()))
                          .getCharContent(false)
                          .toString();
    }
}
