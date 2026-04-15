package io.github.shad3n.predicatemapper.codegen;

import io.github.shad3n.predicatemapper.annotation.Op;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LTE Operator Codegen Tests")
class LteOperatorCodegenOutputTest extends AbstractOperatorCodegenTest {

    @Test
    @DisplayName("Should generate loe() call for Integer field")
    void shouldGenerateLoeCallForIntegerField() throws Exception {
        assertSingleFieldMapping(Integer.class, "stock", "stock", Op.LTE, "loe");
    }
}

