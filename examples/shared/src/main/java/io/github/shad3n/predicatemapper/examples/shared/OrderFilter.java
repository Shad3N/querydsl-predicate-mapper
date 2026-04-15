package io.github.shad3n.predicatemapper.examples.shared;

import io.github.shad3n.predicatemapper.annotation.FilterField;
import io.github.shad3n.predicatemapper.annotation.Op;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Shared filter DTO for Order queries (non-JPA example).
 * <p>
 * Demonstrates using @QueryEntity instead of JPA @Entity.
 */
@Getter
@Setter
public class OrderFilter {

    @FilterField(path = "orderNumber", op = Op.EQ)
    private String orderNumber;

    @FilterField(path = "customer.email", op = Op.LIKE)
    private String customerEmail;

    @FilterField(path = "status", op = Op.IN)
    private List<String> statuses;

    @FilterField(path = "totalAmount", op = Op.GTE)
    private BigDecimal minAmount;

    @FilterField(path = "totalAmount", op = Op.LTE)
    private BigDecimal maxAmount;

    @FilterField(path = "orderDate", op = Op.GTE)
    private LocalDate orderedAfter;

    @FilterField(path = "orderDate", op = Op.LTE)
    private LocalDate orderedBefore;

    @FilterField(path = "cancelled", op = Op.IS_NULL)
    private Boolean notCancelled;
}