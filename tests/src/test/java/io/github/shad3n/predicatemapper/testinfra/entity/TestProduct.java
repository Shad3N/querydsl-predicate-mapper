package io.github.shad3n.predicatemapper.testinfra.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Comprehensive test entity with various field types for integration testing.
 */
@Entity
@Table(name = "test_products")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    private BigDecimal price;

    private Integer stock;

    private Long version;

    private Double weight;

    private Boolean active;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    private LocalDate createdDate;

    private LocalDateTime lastModified;

    private LocalTime openTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private TestCategory category;

    public enum ProductStatus {
        AVAILABLE,
        OUT_OF_STOCK,
        DISCONTINUED,
        PENDING
    }
}