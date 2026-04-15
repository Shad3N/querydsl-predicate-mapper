package io.github.shad3n.predicatemapper.integration.dto;

import io.github.shad3n.predicatemapper.annotation.FilterField;
import io.github.shad3n.predicatemapper.annotation.Op;
import lombok.Data;

import java.util.List;

@Data
public class UserFilter {
    @FilterField(path = "username", op = Op.EQ)
    private String exactUsername;

    @FilterField(path = "age", op = Op.EQ)
    private Integer exactAge;

    @FilterField(path = "username", op = Op.NOT_EQ)
    private String notUsername;

    @FilterField(path = "age", op = Op.LTE)
    private Integer maxAge;

    @FilterField(path = "age", op = Op.GTE)
    private Integer minAge;

    @FilterField(path = "username", op = Op.LIKE)
    private String usernameLike;

    @FilterField(path = "age", op = Op.IN)
    private List<Integer> ageIn;

    @FilterField(path = "email", op = Op.IS_NULL)
    private Boolean emailIsNull;

    @FilterField(path = "email", op = Op.IS_NOT_NULL)
    private Boolean emailIsNotNull;
}
