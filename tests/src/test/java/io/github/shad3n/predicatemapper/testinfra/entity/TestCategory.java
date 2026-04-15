package io.github.shad3n.predicatemapper.testinfra.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Test category entity for testing nested path predicates.
 */
@Entity
@Table(name = "test_categories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private TestCategory parent;

    private Boolean active;
}