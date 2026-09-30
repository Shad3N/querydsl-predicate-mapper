package io.github.shad3n.predicatemapper.javabackend;

import io.github.shad3n.predicatemapper.annotation.FilterField;
import io.github.shad3n.predicatemapper.annotation.Op;
import lombok.Data;

@Data
public class ServerFilter {
    @FilterField(path = "hostname", op = Op.REGEX)
    private String hostnameRegex;

    @FilterField(path = "active", op = Op.EQ)
    private Boolean active;

    @FilterField(path = "location.city", op = Op.EQ, ignoreCase = true)
    private String city;
}
