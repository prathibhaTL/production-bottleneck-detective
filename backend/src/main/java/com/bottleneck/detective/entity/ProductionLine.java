package com.bottleneck.detective.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a manufacturing production line (e.g., "Line A – Automotive Doors").
 * A production line contains many ordered production stages.
 */
@Entity
@Table(name = "production_lines")
@Getter
@Setter
@NoArgsConstructor
public class ProductionLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Production line name is required")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @Size(max = 255)
    private String description;

    /** Active / Inactive / Maintenance */
    @Column(nullable = false, length = 20)
    private String status = "Active";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * One production line has many stages.
     * CascadeType.ALL means if we delete a line, its stages are deleted too.
     * orphanRemoval = true removes stages that are no longer linked to this line.
     */
    @OneToMany(mappedBy = "productionLine", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductionStage> stages = new ArrayList<>();
}
