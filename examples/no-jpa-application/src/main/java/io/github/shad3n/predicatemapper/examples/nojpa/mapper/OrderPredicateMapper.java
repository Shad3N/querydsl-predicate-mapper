package io.github.shad3n.predicatemapper.examples.nojpa.mapper;

import com.querydsl.core.types.Predicate;
import io.github.shad3n.predicatemapper.annotation.PredicateMapper;
import io.github.shad3n.predicatemapper.annotation.ToPredicate;
import io.github.shad3n.predicatemapper.examples.nojpa.entity.QOrder;
import io.github.shad3n.predicatemapper.examples.shared.OrderFilter;

/**
 * Query interface for Order predicates (non-JPA).
 */
@PredicateMapper
public interface OrderPredicateMapper {

    @ToPredicate(QOrder.class)
    Predicate filter(OrderFilter filter);
}