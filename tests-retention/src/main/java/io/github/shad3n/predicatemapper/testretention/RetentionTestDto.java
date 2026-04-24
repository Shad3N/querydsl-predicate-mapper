package io.github.shad3n.predicatemapper.testretention;

import io.github.shad3n.predicatemapper.annotation.FilterField;
import io.github.shad3n.predicatemapper.annotation.Op;
import lombok.Data;

@Data
public class RetentionTestDto {
    @FilterField(path = "username", op = Op.EQ)
    private String username;
}
