package io.github.shad3n.predicatemapper.codegen;

import io.github.shad3n.predicatemapper.annotation.Op;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NOT_EQ Operator Codegen Tests")
class NotEqOperatorCodegenOutputTest extends AbstractOperatorCodegenTest {

    @Test
    @DisplayName("Should generate ne() call for String field")
    void shouldGenerateNeCallForStringField() throws Exception {
        assertSingleFieldMapping(String.class, "name", "name", Op.NOT_EQ, "ne");
    }
}

