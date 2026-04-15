package io.github.shad3n.predicatemapper.examples.jpa.service;

import com.querydsl.core.types.Predicate;
import io.github.shad3n.predicatemapper.examples.jpa.entity.Product;
import io.github.shad3n.predicatemapper.examples.jpa.entity.QProduct;
import io.github.shad3n.predicatemapper.examples.jpa.mapper.ProductPredicateMapper;
import io.github.shad3n.predicatemapper.examples.jpa.repository.ProductRepository;
import io.github.shad3n.predicatemapper.examples.shared.ProductFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service demonstrating predicate mapper usage with JPA repositories.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductPredicateMapper productPredicateMapper;

    /**
     * Search products using filter.
     */
    public List<Product> search(ProductFilter filter) {
        Predicate predicate = productPredicateMapper.filter(filter);
        return productRepository.findAll(predicate);
    }

    /**
     * Search products with pagination.
     */
    public Page<Product> searchPaged(ProductFilter filter, Pageable pageable) {
        Predicate predicate = productPredicateMapper.filter(filter);
        return productRepository.findAll(predicate, pageable);
    }

    /**
     * Compose predicates - user filter + internal constraints.
     */
    public List<Product> searchActiveOnly(ProductFilter filter) {
        Predicate userPredicate = productPredicateMapper.filter(filter);
        Predicate notDeleted = QProduct.product.deletedAt.isNull();
        return productRepository.findAll(userPredicate, notDeleted);
    }
}