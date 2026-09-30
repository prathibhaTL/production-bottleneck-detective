package com.bottleneck.detective.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** Aggregated data for the dashboard summary panel */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {

    private Long productionLineId;
    private String productionLineName;

    private int totalUnitsProduced;
    private int totalDefectiveUnits;
    private double overallDefectRate;          // %
    private double overallDowntimePercent;     // %
    private double productionEfficiency;       // % (good units / theoretical max)

    /** Current detected bottleneck stage name */
    private String currentBottleneckStage;
    private String bottleneckSeverity;

    /** Stage name → capacity utilization % */
    private Map<String, Double> stageUtilization;

    /** Stage name → average queue (waiting time) */
    private Map<String, Double> stageQueueTime;

    /** Hourly production trend: timestamp → units */
    private List<TrendPoint> productionTrend;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TrendPoint {
        private String label;   // e.g. "14:00"
        private int units;
    }
}
