package com.bottleneck.detective.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Stores the result of a bottleneck analysis run.
 *
 * Each time the analysis engine is triggered, it produces one
 * BottleneckAnalysis record that captures which stage was identified
 * as the bottleneck and why.
 */
@Entity
@Table(name = "bottleneck_analyses")
@Getter
@Setter
@NoArgsConstructor
public class BottleneckAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "production_line_id", nullable = false)
    private ProductionLine productionLine;

    /** The stage identified as the primary bottleneck */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bottleneck_stage_id")
    private ProductionStage bottleneckStage;

    /** When this analysis was performed */
    @Column(nullable = false)
    private LocalDateTime analyzedAt = LocalDateTime.now();

    // ----- Key metrics at the time of analysis -----

    private double capacityUtilization;   // 0-100 %
    private double throughput;            // units/hour
    private double avgProcessingTime;     // minutes
    private double avgWaitingTime;        // minutes
    private double downtimePercentage;    // 0-100 %
    private double defectRate;            // 0-100 %
    private double queueBuildup;          // queued units

    /**
     * Overall severity score (0–100).
     * Higher = more severe bottleneck.
     */
    private double severityScore;

    /**
     * Human-readable severity label: LOW / MEDIUM / HIGH / CRITICAL
     */
    @Column(length = 20)
    private String severityLabel;

    /**
     * Free-text explanation of which factors contributed to this bottleneck.
     * Stored as a JSON array string for simplicity.
     * Example: ["Excessive downtime (32%)", "High processing time (12 min avg)"]
     */
    @Column(columnDefinition = "TEXT")
    private String contributingFactors;

    /**
     * Free-text summary of the analysis result – shown to users on the dashboard.
     */
    @Column(columnDefinition = "TEXT")
    private String summary;
}
