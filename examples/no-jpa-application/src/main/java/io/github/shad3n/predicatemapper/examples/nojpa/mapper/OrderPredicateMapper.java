package io.github.shad3n.predicatemapper.examples.nojpa.mapper;

import com.querydsl.core.types.Predicate;
import io.github.shad3n.predicatemapper.annotation.PredicateMapper;
import io.github.shad3n.predicatemapper.annotation.ToQueryDslPredicateMapper;
import io.github.shad3n.predicatemapper.examples.nojpa.entity.QOrder;
import io.github.shad3n.predicatemapper.examples.shared.OrderFilter;

/**
 * Query interface for Order predicates (non-JPA).
 */
@PredicateMapper
public interface OrderPredicateMapper {

    @ToQueryDslPredicateMapper(QOrder.class)
    Predicate filter(OrderFilter filter);
}