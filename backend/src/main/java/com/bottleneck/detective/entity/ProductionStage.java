package com.bottleneck.detective.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single stage within a production line (e.g., "Welding", "Painting", "Assembly").
 * Each stage belongs to one production line and is handled by one machine.
 */
@Entity
@Table(name = "production_stages")
@Getter
@Setter
@NoArgsConstructor
public class ProductionStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Stage name is required")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    /** Stage order within its production line (1 = first stage) */
    @Column(nullable = false)
    private int stageOrder = 1;

    /**
     * Maximum units this stage can process per hour under ideal conditions.
     * Used to calculate capacity utilization.
     */
    @Positive(message = "Capacity must be positive")
    @Column(nullable = false)
    private double capacityPerHour;

    /**
     * Ideal processing time in minutes to handle one unit.
     */
    @Positive
    @Column(nullable = false)
    private double standardProcessingTimeMinutes;

    /** Active / Idle / Maintenance / Down */
    @Column(nullable = false, length = 20)
    private String status = "Active";

    /** The production line this stage belongs to */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_line_id", nullable = false)
    private ProductionLine productionLine;

    /** The machine assigned to this stage */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;
}
