package io.github.shad3n.predicatemapper.examples.jpa.mapper;

import com.querydsl.core.types.Predicate;
import io.github.shad3n.predicatemapper.annotation.PredicateMapper;
import io.github.shad3n.predicatemapper.annotation.ToPredicate;
import io.github.shad3n.predicatemapper.examples.jpa.entity.QProduct;
import io.github.shad3n.predicatemapper.examples.shared.ProductFilter;

/**
 * Query interface for Product predicates.
 * <p>
 * The APT generates ProductQueriesImpl implementing this interface.
 * Compile-time validation ensures all paths exist on QProduct.
 */
@PredicateMapper
public interface ProductPredicateMapper {

    @ToPredicate(QProduct.class)
    Predicate filter(ProductFilter filter);
}