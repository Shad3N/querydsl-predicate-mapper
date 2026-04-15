package io.github.shad3n.predicatemapper.testinfra;

import com.google.testing.compile.JavaFileObjects;
import com.palantir.javapoet.*;
import io.github.shad3n.predicatemapper.annotation.FilterField;
import io.github.shad3n.predicatemapper.annotation.Op;
import io.github.shad3n.predicatemapper.annotation.PredicateMapper;
import io.github.shad3n.predicatemapper.annotation.ToPredicate;

import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating test source files for annotation processor tests.
 * Provides fluent builders for constructing DTOs and mapper interfaces using JavaPoet.
 */
public final class TestSourceFactory {

    private TestSourceFactory() {
    }

    // ==================== Spring Annotation ====================

    /**
     * Creates Spring's @Component annotation source for testing Spring integration.
     */
    public static JavaFileObject componentAnnotation() {
        return JavaFileObjects.forSourceString("org.springframework.stereotype.Component",
                                               """
                                                       package org.springframework.stereotype;
                                                       public @interface Component {}
                                                       """);
    }

    // ==================== DTO Builder ====================

    public static DtoBuilder dto(String packageName, String className) {
        return new DtoBuilder(packageName, className);
    }

    public static MapperBuilder mapper(String packageName, String interfaceName) {
        return new MapperBuilder(packageName, interfaceName);
    }

    // ==================== Mapper Interface Builder ====================

    public static TestSetupBuilder setup() {
        return new TestSetupBuilder();
    }

    public static class DtoBuilder {
        private final String packageName;
        private final TypeSpec.Builder classBuilder;

        private DtoBuilder(String packageName, String className) {
            this.packageName = packageName;
            this.classBuilder = TypeSpec.classBuilder(className)
                                        .addModifiers(Modifier.PUBLIC);
        }

        public DtoBuilder field(String name, TypeName type, String path, Op op) {
            AnnotationSpec filterField = AnnotationSpec.builder(FilterField.class)
                                                       .addMember("path", "$S", path)
                                                       .addMember("op", "$T.$L", Op.class, op.name())
                                                       .build();

            FieldSpec field = FieldSpec.builder(type, name)
                                       .addModifiers(Modifier.PRIVATE)
                                       .addAnnotation(filterField)
                                       .build();

            MethodSpec getter = MethodSpec.methodBuilder("get" + capitalize(name))
                                          .addModifiers(Modifier.PUBLIC)
                                          .returns(type)
                                          .addStatement("return $N", name)
                                          .build();

            MethodSpec setter = MethodSpec.methodBuilder("set" + capitalize(name))
                                          .addModifiers(Modifier.PUBLIC)
                                          .returns(void.class)
                                          .addParameter(type, name)
                                          .addStatement("this.$N = $N", name, name)
                                          .build();

            classBuilder.addField(field).addMethod(getter).addMethod(setter);
            return this;
        }

        public DtoBuilder field(String name, Class<?> type, String path, Op op) {
            return field(name, TypeName.get(type), path, op);
        }

        public JavaFileObject build() {
            JavaFile javaFile = JavaFile.builder(packageName, classBuilder.build()).build();
            return javaFile.toJavaFileObject();
        }

        private String capitalize(String s) {
            return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
    }

    // ==================== Test Setup Builder ====================

    public static class MapperBuilder {
        private final String packageName;
        private final TypeSpec.Builder interfaceBuilder;

        private MapperBuilder(String packageName, String interfaceName) {
            this.packageName = packageName;
            this.interfaceBuilder = TypeSpec.interfaceBuilder(interfaceName)
                                            .addModifiers(Modifier.PUBLIC)
                                            .addAnnotation(PredicateMapper.class);
        }

        public MapperBuilder method(String methodName, ClassName qClass, ClassName dtoClass) {
            AnnotationSpec toPredicate = AnnotationSpec.builder(ToPredicate.class)
                                                       .addMember("value", "$T.class", qClass)
                                                       .build();

            MethodSpec method = MethodSpec.methodBuilder(methodName)
                                          .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                                          .addAnnotation(toPredicate)
                                          .returns(ClassName.get("com.querydsl.core.types", "Predicate"))
                                          .addParameter(dtoClass, "dto")
                                          .build();

            interfaceBuilder.addMethod(method);
            return this;
        }

        public JavaFileObject build() {
            JavaFile javaFile = JavaFile.builder(packageName, interfaceBuilder.build()).build();
            return javaFile.toJavaFileObject();
        }
    }

    public static class TestSetupBuilder {
        private final List<JavaFileObject> extraSources = new ArrayList<>();
        private String packageName = "test";
        private boolean withSpring = false;

        public TestSetupBuilder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public TestSetupBuilder withSpring() {
            this.withSpring = true;
            return this;
        }

        public TestSetupBuilder extraSource(JavaFileObject source) {
            extraSources.add(source);
            return this;
        }

        public TestSetupResult build(DtoBuilder dto, MapperBuilder mapper) {
            List<JavaFileObject> allSources = new ArrayList<>();

            allSources.addAll(extraSources);

            if (withSpring) {
                allSources.add(componentAnnotation());
            }

            allSources.add(dto.build());
            allSources.add(mapper.build());

            return new TestSetupResult(allSources, packageName);
        }
    }

    public record TestSetupResult(List<JavaFileObject> sources, String packageName) {
    }
}
