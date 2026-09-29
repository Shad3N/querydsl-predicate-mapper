package io.github.shad3n.predicatemapper.integration.mapper;

import com.querydsl.core.types.Predicate;
import io.github.shad3n.predicatemapper.annotation.PredicateMapper;
import io.github.shad3n.predicatemapper.annotation.ToQueryDslPredicateMapper;
import io.github.shad3n.predicatemapper.integration.dto.UserFilter;
import io.github.shad3n.predicatemapper.integration.entity.QUser;

@PredicateMapper
public interface UserPredicateMapper {
    @ToQueryDslPredicateMapper(QUser.class)
    Predicate filter(UserFilter filter);
}
