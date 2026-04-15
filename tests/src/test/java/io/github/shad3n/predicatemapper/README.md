# QueryDSL Predicate Mapper - Test Infrastructure Guide

This guide explains the philosophy and approach to writing tests for the QueryDSL Predicate Mapper annotation processor.
We use a dynamic, elegant approach to testing that avoids messy static files and brittle string matching.

## The Philosophy

Our testing approach is built on three core pillars:

1. **Dynamic Source Generation:** Instead of maintaining static `.java` files in `src/test/resources`, we use **JavaPoet
   ** to generate test inputs (DTOs, Q-classes, and Mappers) dynamically within the test methods. This keeps the tests
   self-contained, easier to read, and much more flexible for testing various edge cases.
2. **Real Classpath:** We compile against the actual project dependencies (like QueryDSL and Spring) instead of relying
   on manually written mock stubs. This ensures our tests accurately reflect real-world usage.
3. **AST-Based Assertions:** We parse the generated code using **JavaParser** and assert on its structure using a custom
   **AssertJ** assertion class (`GeneratedCodeAssert`). This is far more robust than regex or simple string matching, as
   it understands the actual Java syntax.

## How to Write a Test (Conceptual Guide)

When writing a new annotation processor test, follow this general workflow:

### 1. Generate the DTO

Use the `TestSourceFactory` to build the filter DTO you want to test. The factory provides a fluent builder to add
fields with specific types, paths, and QueryDSL operators. You can define primitive fields, object fields, and even
parameterized types like collections.

### 2. Generate the Mapper Interface

Similarly, use the `TestSourceFactory` to define the interface that the annotation processor will implement. You will
need to specify the target Q-class and the DTO class you generated in the previous step, and define the method
signature.

### 3. Build the Test Setup

Combine the generated sources using the setup builder provided by `TestSourceFactory`. If your test requires specific
integration features (like testing the `@Component` annotation for Spring), you can configure the setup builder to
include those necessary stubs or annotations.

### 4. Compile

Use the `compile()` method inherited from `AbstractProcessorTest` to run the annotation processor against your generated
sources. Assert that the compilation succeeded and that the expected implementation file was generated.

### 5. Assert on the Generated Code Structure

Retrieve the generated source code as a string and use the custom `GeneratedCodeAssert` (typically statically imported
as `assertThat`) to verify the structure and logic.

The assertion class provides a fluent API to check for things like:

* Presence or absence of specific annotations (e.g., `@Component`).
* Implemented interfaces.
* Presence of specific methods and whether they have `@Override`.
* The number of `if` statements within a method (useful for verifying all fields are processed).
* Specific logic within methods, such as null checks, operator usage, or the creation of specific objects like
  `BooleanBuilder`.

By focusing on the AST structure rather than exact string matches, your tests will be much more resilient to minor
formatting changes in the generated code.