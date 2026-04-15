package io.github.shad3n.predicatemapper.codegen;

import io.github.shad3n.predicatemapper.annotation.Op;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GTE Operator Codegen Tests")
class GteOperatorCodegenOutputTest extends AbstractOperatorCodegenTest {

    @Test
    @DisplayName("Should generate goe() call for Integer field")
    void shouldGenerateGoeCallForIntegerField() throws Exception {
        assertSingleFieldMapping(Integer.class, "stock", "stock", Op.GTE, "goe");
    }
}

