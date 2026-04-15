package io.github.shad3n.predicatemapper.examples.sender.service;

import io.github.shad3n.predicatemapper.examples.sender.client.ProductClient;
import io.github.shad3n.predicatemapper.examples.shared.ProductFilter;
import io.github.shad3n.predicatemapper.examples.shared.ProductStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service demonstrating sender-side filter construction.
 * <p>
 * This service:
 * 1. Constructs ProductFilter with type-safe fields
 * 2. Sends to receiver via HTTP
 * 3. Receiver converts filter to QueryDSL predicate
 * <p>
 * Both sides use the same shared ProductFilter class,
 * ensuring compile-time safety across service boundaries.
 */
@Service
@RequiredArgsConstructor
public class ProductSenderService {

    private final ProductClient productClient;

    /**
     * Search products by name pattern.
     */
    public List<Map<String, Object>> searchByName(String namePattern) {
        ProductFilter filter = new ProductFilter();
        filter.setName(namePattern);
        filter.setActiveOnly(true);

        return productClient.searchByFilter(filter);
    }

    /**
     * Search products in price range.
     */
    public List<Map<String, Object>> searchByPriceRange(BigDecimal min, BigDecimal max) {
        ProductFilter filter = new ProductFilter();
        filter.setMinPrice(min);
        filter.setMaxPrice(max);
        filter.setActiveOnly(true);

        return productClient.searchByFilter(filter);
    }

    /**
     * Search available products by status.
     */
    public List<Map<String, Object>> searchByStatuses(List<ProductStatus> statuses) {
        ProductFilter filter = new ProductFilter();
        filter.setStatuses(statuses);
        filter.setActiveOnly(true);

        return productClient.searchByFilter(filter);
    }

    /**
     * Search products by category.
     */
    public List<Map<String, Object>> searchByCategory(String categoryName) {
        ProductFilter filter = new ProductFilter();
        filter.setCategoryName(categoryName);
        filter.setActiveOnly(true);

        return productClient.searchByFilter(filter);
    }

    /**
     * Complex search combining multiple criteria.
     */
    public List<Map<String, Object>> complexSearch(
            String namePattern,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            List<ProductStatus> statuses,
            String categoryName
    ) {
        ProductFilter filter = new ProductFilter();
        filter.setName(namePattern);
        filter.setMinPrice(minPrice);
        filter.setMaxPrice(maxPrice);
        filter.setStatuses(statuses);
        filter.setCategoryName(categoryName);
        filter.setActiveOnly(true);

        return productClient.searchByFilter(filter);
    }
}