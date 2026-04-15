package io.github.shad3n.predicatemapper.testinfra;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import org.assertj.core.api.AbstractAssert;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * AssertJ assertion for generated Java source code.
 * Uses JavaParser to create an AST for structural assertions instead of string matching.
 */
public class GeneratedCodeAssert extends AbstractAssert<GeneratedCodeAssert, String> {

    private final CompilationUnit cu;
    private final ClassOrInterfaceDeclaration mainClass;

    protected GeneratedCodeAssert(String sourceCode) {
        super(sourceCode, GeneratedCodeAssert.class);

        if (sourceCode == null || sourceCode.isBlank()) {
            failWithMessage("Source code is null or blank");
        }

        JavaParser parser = new JavaParser();
        this.cu = parser.parse(sourceCode).getResult()
                        .orElseThrow(() -> new AssertionError("Failed to parse source code:\n" + sourceCode));

        this.mainClass = (ClassOrInterfaceDeclaration) cu.getTypes().stream()
                                                         .filter(t -> t instanceof ClassOrInterfaceDeclaration)
                                                         .findFirst()
                                                         .orElseThrow(() -> new AssertionError(
                                                                 "No class found in compilation unit"));
    }

    public static GeneratedCodeAssert assertThat(String sourceCode) {
        return new GeneratedCodeAssert(sourceCode);
    }

    public GeneratedCodeAssert hasAnnotation(String annotationName) {
        isNotNull();
        boolean hasAnnotation = mainClass.getAnnotations().stream()
                                         .anyMatch(a -> a.getNameAsString().equals(annotationName) ||
                                                 a.getNameAsString().endsWith("." + annotationName));
        if (!hasAnnotation) {
            failWithMessage("Expected class to have @%s annotation", annotationName);
        }
        return this;
    }

    public GeneratedCodeAssert implementsInterface(String interfaceName) {
        isNotNull();
        boolean implementsInterface = mainClass.getImplementedTypes().stream()
                                               .anyMatch(t -> t.getNameAsString().equals(interfaceName));
        if (!implementsInterface) {
            failWithMessage("Expected class to implement %s", interfaceName);
        }
        return this;
    }

    private Optional<MethodDeclaration> getMethod(String methodName) {
        return mainClass.getMethods().stream()
                        .filter(m -> m.getNameAsString().equals(methodName))
                        .findFirst();
    }

    public GeneratedCodeAssert hasMethod(String methodName) {
        isNotNull();
        if (getMethod(methodName).isEmpty()) {
            failWithMessage("Expected method: %s", methodName);
        }
        return this;
    }

    private String getMethodBody(String methodName) {
        MethodDeclaration method = getMethod(methodName)
                .orElseThrow(() -> new AssertionError("Method not found: " + methodName));
        return method.getBody().map(BlockStmt::toString).orElse("");
    }

    public GeneratedCodeAssert methodContainsCondition(String methodName, String conditionSubstring) {
        isNotNull();
        String body = getMethodBody(methodName);
        if (!body.contains(conditionSubstring)) {
            failWithMessage("Expected method %s to contain condition: %s\nActual body:\n%s",
                            methodName, conditionSubstring, body);
        }
        return this;
    }

    public GeneratedCodeAssert methodContainsStatement(String methodName, String statementSubstring) {
        isNotNull();
        String body = getMethodBody(methodName);
        if (!body.contains(statementSubstring)) {
            failWithMessage("Expected method %s to contain statement: %s\nActual body:\n%s",
                            methodName, statementSubstring, body);
        }
        return this;
    }

    public GeneratedCodeAssert hasIfStatementCount(String methodName, int expectedCount) {
        isNotNull();
        MethodDeclaration method = getMethod(methodName)
                .orElseThrow(() -> new AssertionError("Method not found: " + methodName));
        int actualCount = method.getBody()
                                .map(body -> body.findAll(IfStmt.class))
                                .orElse(List.of()).size();

        if (actualCount != expectedCount) {
            failWithMessage("Expected %d if statements in %s but found %d",
                            expectedCount, methodName, actualCount);
        }
        return this;
    }

    public GeneratedCodeAssert createsQClass(String methodName, String qClassName) {
        isNotNull();
        String body = getMethodBody(methodName);
        if (!body.contains("new " + qClassName + "(") && !body.contains("new " + qClassName + " (")) {
            failWithMessage("Expected method %s to create Q-class: %s\nActual body:\n%s",
                            methodName, qClassName, body);
        }
        return this;
    }

    public GeneratedCodeAssert createsBooleanBuilder(String methodName) {
        isNotNull();
        String body = getMethodBody(methodName);
        if (!body.contains("new BooleanBuilder()")) {
            failWithMessage("Expected method %s to create BooleanBuilder\nActual body:\n%s",
                            methodName, body);
        }
        return this;
    }

    public GeneratedCodeAssert returnsPredicate(String methodName) {
        isNotNull();
        String body = getMethodBody(methodName);
        if (!body.contains("return") || !body.contains("Expressions.TRUE")) {
            failWithMessage("Expected method %s to return predicate with TRUE fallback\nActual body:\n%s",
                            methodName, body);
        }
        return this;
    }

    public GeneratedCodeAssert hasNullCheck(String methodName, String fieldName) {
        isNotNull();
        String body = getMethodBody(methodName);
        String getter = "get" + capitalize(fieldName) + "()";
        if (!body.contains(getter + " != null") && !body.contains("TRUE.equals(" + getter + ")")) {
            failWithMessage("Expected null check for field %s in method %s\nActual body:\n%s",
                            fieldName, methodName, body);
        }
        return this;
    }

    public GeneratedCodeAssert hasOperatorUsage(String methodName, String path, String operator) {
        isNotNull();
        String body = getMethodBody(methodName);
        String expectedOp = "." + operator + "(";
        if (!body.contains(path + expectedOp) && !body.contains("q." + path + expectedOp)) {
            failWithMessage("Expected operator %s on path %s in method %s\nActual body:\n%s",
                            operator, path, methodName, body);
        }
        return this;
    }

    public GeneratedCodeAssert hasMethodsExactly(String... methodNames) {
        isNotNull();
        List<String> actual = mainClass.getMethods().stream()
                                       .map(MethodDeclaration::getNameAsString)
                                       .collect(Collectors.toList());
        List<String> expected = List.of(methodNames);

        if (!actual.equals(expected)) {
            failWithMessage("Method names mismatch. Expected: %s, Actual: %s", expected, actual);
        }
        return this;
    }

    private String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
