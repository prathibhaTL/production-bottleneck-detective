package com.bottleneck.detective.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A single observation captured for a stage during production.
 *
 * Think of this as a row in a production log – one entry per
 * stage per shift (or per time window, depending on how often
 * data is collected).
 *
 * All time fields use minutes.
 */
@Entity
@Table(name = "production_records")
@Getter
@Setter
@NoArgsConstructor
public class ProductionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ----- Relationships -----

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_line_id", nullable = false)
    private ProductionLine productionLine;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    private ProductionStage stage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private Machine machine;

    // ----- Time of observation -----

    @NotNull
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();

    /** Morning / Afternoon / Night */
    @Column(length = 20)
    private String shift;

    // ----- Quantities -----

    /** Total units fed into this stage during the observation window */
    @Min(0)
    @Column(nullable = false)
    private int unitsProduced;

    /** Number of units that failed quality checks */
    @Min(0)
    @Column(nullable = false)
    private int defectiveUnits;

    // ----- Time metrics (minutes) -----

    /** Average time to process one unit (minutes) */
    @Min(0)
    @Column(nullable = false)
    private double processingTimeMinutes;

    /**
     * Average time a unit spent waiting before processing began (minutes).
     * High waiting time = units are piling up before this stage → queue buildup.
     */
    @Min(0)
    @Column(nullable = false)
    private double waitingTimeMinutes;

    /**
     * Total time the machine was down (not producing) during this window (minutes).
     */
    @Min(0)
    @Column(nullable = false)
    private double downtimeMinutes;

    /**
     * Total duration of the observation window (minutes).
     * Used to calculate downtime percentage.
     * Default is 480 minutes (one 8-hour shift).
     */
    @Column(nullable = false)
    private double totalWindowMinutes = 480.0;
}
