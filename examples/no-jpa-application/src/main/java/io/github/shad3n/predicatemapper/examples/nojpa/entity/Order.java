package io.github.shad3n.predicatemapper.examples.nojpa.entity;

import com.querydsl.core.annotations.QueryEntity;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Order entity using @QueryEntity (no JPA).
 */
@Setter
@Getter
@QueryEntity
public class Order {
    private String orderNumber;
    private Customer customer;
    private String status;
    private BigDecimal totalAmount;
    private LocalDate orderDate;
    private LocalDate cancelled;

}