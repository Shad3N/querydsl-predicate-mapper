package io.github.shad3n.predicatemapper.codegen;

import io.github.shad3n.predicatemapper.annotation.Op;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

@DisplayName("IN Operator Codegen Tests")
class InOperatorCodegenOutputTest extends AbstractOperatorCodegenTest {

    @Test
    @DisplayName("Should generate in() call for Collection field")
    void shouldGenerateInCallForCollectionField() throws Exception {
        assertSingleFieldMapping(List.class, "names", "name", Op.IN, "in");
    }
}

