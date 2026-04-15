package io.github.shad3n.predicatemapper.integration.mapper;

import com.querydsl.core.types.Predicate;
import io.github.shad3n.predicatemapper.annotation.PredicateMapper;
import io.github.shad3n.predicatemapper.annotation.ToPredicate;
import io.github.shad3n.predicatemapper.integration.dto.UserFilter;
import io.github.shad3n.predicatemapper.integration.entity.QUser;

@PredicateMapper
public interface UserPredicateMapper {
    @ToPredicate(QUser.class)
    Predicate filter(UserFilter filter);
}
