package io.github.shad3n.predicatemapper.examples.sender.runner;

import io.github.shad3n.predicatemapper.examples.sender.client.ProductClient;
import io.github.shad3n.predicatemapper.examples.shared.ProductFilter;
import io.github.shad3n.predicatemapper.examples.shared.ProductStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Automatically executes demo queries on application startup,
 * logs results to console, then shuts down the application.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryRunner implements CommandLineRunner {

    private final ProductClient productClient;
    private final ApplicationContext applicationContext;

    @Override
    public void run(String... args) {
        log.info("=== QueryDSL Predicate Mapper - Query Sender Demo ===");
        log.info("Executing queries against receiver service at localhost:8081");
        log.info("");

        executeDemoQueries();

        log.info("");
        log.info("=== Demo complete. Shutting down... ===");

        // Graceful shutdown
        System.exit(0);
    }

    private void executeDemoQueries() {
        searchByNamePattern();
        searchByPriceRange();
        searchByStatus();
        searchByCategory();
        complexSearch();
    }

    private void searchByNamePattern() {
        log.info("--- Query 1: Search by name pattern ---");

        ProductFilter filter = new ProductFilter();
        filter.setName("widget");
        filter.setActiveOnly(true);

        log.info("Filter: name='{}', activeOnly={}", filter.getName(), filter.getActiveOnly());

        try {
            List<Map<String, Object>> results = productClient.searchByFilter(filter);
            log.info("Results: {} products found", results.size());
            results.forEach(this::logProduct);
        } catch (Exception e) {
            log.warn("Query failed (receiver service may not be running): {}", e.getMessage());
        }
        log.info("");
    }

    private void searchByPriceRange() {
        log.info("--- Query 2: Search by price range ---");

        ProductFilter filter = new ProductFilter();
        filter.setMinPrice(new BigDecimal("10.00"));
        filter.setMaxPrice(new BigDecimal("100.00"));
        filter.setActiveOnly(true);

        log.info("Filter: minPrice={}, maxPrice={}, activeOnly={}",
                 filter.getMinPrice(), filter.getMaxPrice(), filter.getActiveOnly());

        try {
            List<Map<String, Object>> results = productClient.searchByFilter(filter);
            log.info("Results: {} products found", results.size());
            results.forEach(this::logProduct);
        } catch (Exception e) {
            log.warn("Query failed (receiver service may not be running): {}", e.getMessage());
        }
        log.info("");
    }

    private void searchByStatus() {
        log.info("--- Query 3: Search by status ---");

        ProductFilter filter = new ProductFilter();
        filter.setStatuses(List.of(ProductStatus.AVAILABLE, ProductStatus.PENDING_REVIEW));
        filter.setActiveOnly(true);

        log.info("Filter: statuses={}, activeOnly={}", filter.getStatuses(), filter.getActiveOnly());

        try {
            List<Map<String, Object>> results = productClient.searchByFilter(filter);
            log.info("Results: {} products found", results.size());
            results.forEach(this::logProduct);
        } catch (Exception e) {
            log.warn("Query failed (receiver service may not be running): {}", e.getMessage());
        }
        log.info("");
    }

    private void searchByCategory() {
        log.info("--- Query 4: Search by category ---");

        ProductFilter filter = new ProductFilter();
        filter.setCategoryName("Electronics");
        filter.setActiveOnly(true);

        log.info("Filter: categoryName='{}', activeOnly={}", filter.getCategoryName(), filter.getActiveOnly());

        try {
            List<Map<String, Object>> results = productClient.searchByFilter(filter);
            log.info("Results: {} products found", results.size());
            results.forEach(this::logProduct);
        } catch (Exception e) {
            log.warn("Query failed (receiver service may not be running): {}", e.getMessage());
        }
        log.info("");
    }

    private void complexSearch() {
        log.info("--- Query 5: Complex search (multiple criteria) ---");

        ProductFilter filter = new ProductFilter();
        filter.setName("pro");
        filter.setMinPrice(new BigDecimal("50.00"));
        filter.setMaxPrice(new BigDecimal("500.00"));
        filter.setStatuses(List.of(ProductStatus.AVAILABLE));
        filter.setActiveOnly(true);

        log.info("Filter: name='{}', priceRange=[{}, {}], statuses={}, activeOnly={}",
                 filter.getName(), filter.getMinPrice(), filter.getMaxPrice(),
                 filter.getStatuses(), filter.getActiveOnly());

        try {
            List<Map<String, Object>> results = productClient.searchByFilter(filter);
            log.info("Results: {} products found", results.size());
            results.forEach(this::logProduct);
        } catch (Exception e) {
            log.warn("Query failed (receiver service may not be running): {}", e.getMessage());
        }
        log.info("");
    }

    private void logProduct(Map<String, Object> product) {
        log.info("  Product: id={}, name={}, price={}, status={}",
                 product.get("id"), product.get("name"), product.get("price"), product.get("status"));
    }
}