package io.github.shad3n.predicatemapper.examples.nojpa.entity;

import com.querydsl.core.annotations.QueryEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * Customer entity using @QueryEntity (no JPA).
 */
@Setter
@Getter
@QueryEntity
public class Customer {
    private String email;
    private String name;

}