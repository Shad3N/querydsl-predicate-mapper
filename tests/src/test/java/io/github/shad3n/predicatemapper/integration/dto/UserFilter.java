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

    @FilterField(path = "username", op = Op.EQ, ignoreCase = true)
    private String usernameIgnoringCase;

    @FilterField(path = "username", op = Op.NOT_EQ, ignoreCase = true)
    private String notUsernameIgnoringCase;

    @FilterField(path = "username", op = Op.LIKE, ignoreCase = true)
    private String usernameLikeIgnoringCase;

    @FilterField(path = "username", op = Op.CONTAINS)
    private String usernameContains;

    @FilterField(path = "username", op = Op.CONTAINS, ignoreCase = true)
    private String usernameContainsIgnoringCase;
}
