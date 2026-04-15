package io.github.shad3n.predicatemapper.examples.jpa.repository;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import io.github.shad3n.predicatemapper.examples.jpa.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;
import java.util.UUID;

/**
 * Product repository with QueryDSL predicate support.
 */
public interface ProductRepository extends JpaRepository<Product, UUID>, QuerydslPredicateExecutor<Product> {

    @Override
    List<Product> findAll(Predicate predicate);

    @Override
    List<Product> findAll(Predicate predicate, com.querydsl.core.types.OrderSpecifier<?>... orders);

    /**
     * Find all entities matching all given predicates (AND composition).
     * Predicates are combined using ExpressionUtils.allOf().
     *
     * @param predicates variable number of predicates to combine with AND
     * @return list of matching entities
     */
    default List<Product> findAll(Predicate... predicates) {
        return findAll(ExpressionUtils.allOf(predicates));
    }
}