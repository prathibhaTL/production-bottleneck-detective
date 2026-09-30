package com.bottleneck.detective.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The full result returned by the bottleneck analysis endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BottleneckResult {

    private Long productionLineId;
    private String productionLineName;

    /** The stage identified as the primary bottleneck */
    private StageMetrics bottleneckStage;

    /** A plain-English label: LOW / MEDIUM / HIGH / CRITICAL */
    private String severityLabel;

    /** Numeric severity 0-100 */
    private double severityScore;

    /** One-paragraph summary suitable for display on the dashboard */
    private String summary;

    /**
     * Metrics for ALL stages, sorted by bottleneck score descending.
     * The frontend uses this to draw the stage-utilization bar chart.
     */
    private List<StageMetrics> allStageMetrics;
}
