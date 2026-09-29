package io.github.shad3n.predicatemapper.integration;

import com.querydsl.core.types.Predicate;
import io.github.shad3n.predicatemapper.annotation.PredicateMapper;
import io.github.shad3n.predicatemapper.annotation.ToQueryDslPredicateMapper;
import io.github.shad3n.predicatemapper.testretention.RetentionTestDto;

@PredicateMapper
public interface RetentionTestMapper {
    @ToQueryDslPredicateMapper(io.github.shad3n.predicatemapper.integration.entity.QUser.class)
    Predicate createPredicate(RetentionTestDto filter);
}
