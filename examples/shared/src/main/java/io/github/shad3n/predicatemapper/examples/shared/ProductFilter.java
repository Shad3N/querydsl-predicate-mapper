package io.github.shad3n.predicatemapper.examples.shared;

import io.github.shad3n.predicatemapper.annotation.FilterField;
import io.github.shad3n.predicatemapper.annotation.Op;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Shared filter DTO for Product queries.
 * <p>
 * This class is used by:
 * - Sender service: constructs filter, serializes to query params
 * - Receiver service: deserializes, converts to QueryDSL predicate
 * <p>
 * Both services get compile-time validation of Q-class paths.
 */
@Getter
@Setter
public class ProductFilter {

    // LIKE - pattern matching
    @FilterField(path = "name", op = Op.LIKE)
    private String name;

    // Range queries - same field, different operators
    @FilterField(path = "price", op = Op.GTE)
    private BigDecimal minPrice;

    @FilterField(path = "price", op = Op.LTE)
    private BigDecimal maxPrice;

    // Equality on enum
    @FilterField(path = "status", op = Op.EQ)
    private ProductStatus status;

    // IN clause for multiple values
    @FilterField(path = "status", op = Op.IN)
    private List<ProductStatus> statuses;

    // Nested path - traverse relationships
    @FilterField(path = "category.name", op = Op.EQ)
    private String categoryName;

    @FilterField(path = "category.id", op = Op.IN)
    private List<UUID> categoryIds;

    // Null check - filter soft-deleted records
    @FilterField(path = "deletedAt", op = Op.IS_NULL)
    private Boolean activeOnly;

    // Date range queries
    @FilterField(path = "createdAt", op = Op.GTE)
    private LocalDateTime createdAfter;

    @FilterField(path = "createdAt", op = Op.LTE)
    private LocalDateTime createdBefore;

    // Pagination fields - no annotation, ignored by predicate mapper
    private Integer page;
    private Integer size;
}