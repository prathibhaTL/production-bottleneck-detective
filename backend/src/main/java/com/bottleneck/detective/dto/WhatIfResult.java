package com.bottleneck.detective.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The estimated outcome of a what-if scenario.
 * All results are clearly labelled as ESTIMATES.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatIfResult {

    // ─── Inputs echoed back ───────────────────────────────────────────────
    private String targetStageName;
    private double downtimeReductionPercent;
    private double processingTimeReductionPercent;
    private double additionalCapacityUnitsPerHour;
    private double defectRateReductionPercent;

    // ─── Baseline (before changes) ───────────────────────────────────────
    private double baselineThroughput;          // units/hour
    private double baselineDowntimePercent;
    private double baselineProcessingTime;      // minutes/unit
    private double baselineDefectRate;          // %

    // ─── Estimated outcome ───────────────────────────────────────────────
    private double estimatedThroughput;         // units/hour
    private double estimatedDowntimePercent;
    private double estimatedProcessingTime;
    private double estimatedDefectRate;

    /** Absolute throughput improvement estimate */
    private double throughputImprovementUnitsPerHour;

    /** Relative throughput improvement estimate (%) */
    private double throughputImprovementPercent;

    /**
     * A plain-English note reminding the user these are estimates.
     * Important for honest communication of uncertainty.
     */
    private String disclaimer = "ESTIMATE ONLY: These projections are based on simplified linear " +
            "assumptions and historical data averages. Actual results will vary depending on " +
            "equipment condition, workforce skill, material availability, and other factors.";
}
