package io.github.shad3n.predicatemapper.codegen;

import io.github.shad3n.predicatemapper.annotation.Op;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LIKE Operator Codegen Tests")
class LikeOperatorCodegenOutputTest extends AbstractOperatorCodegenTest {

    @Test
    @DisplayName("Should generate like() call for String field")
    void shouldGenerateLikeCallForStringField() throws Exception {
        assertSingleFieldMapping(String.class, "name", "name", Op.LIKE, "like");
    }
}

