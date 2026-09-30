package com.bottleneck.detective.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The output of a single stage's analysis.
 * One of these is produced for every stage and they are compared
 * to find the worst (bottleneck) stage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageMetrics {

    private Long stageId;
    private String stageName;

    // ----- Computed metrics -----

    /**
     * capacityUtilization = (unitsProduced / capacityPerHour) * 100
     * A value above 90% means the stage is running near its limit.
     */
    private double capacityUtilization;   // %

    /**
     * throughput = unitsProduced / windowHours
     */
    private double throughput;            // units/hour

    /**
     * Average processing time across all records for this stage.
     */
    private double avgProcessingTime;     // minutes/unit

    /**
     * Average waiting time – a proxy for queue buildup in front of this stage.
     */
    private double avgWaitingTime;        // minutes

    /**
     * downtimePercentage = (totalDowntimeMinutes / totalWindowMinutes) * 100
     */
    private double downtimePercentage;    // %

    /**
     * defectRate = (defectiveUnits / unitsProduced) * 100
     */
    private double defectRate;            // %

    /**
     * A normalised 0-100 score calculated by the bottleneck engine.
     * Higher = more likely to be the bottleneck.
     */
    private double bottleneckScore;

    /**
     * Possible factors that contributed to this score.
     * We say "possible" because correlation ≠ causation.
     */
    private List<String> contributingFactors;
}
