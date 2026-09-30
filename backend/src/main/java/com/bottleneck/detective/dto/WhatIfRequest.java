package com.bottleneck.detective.dto;

import lombok.Data;

/**
 * The user's "what-if" scenario inputs.
 *
 * Each field is a PERCENTAGE REDUCTION relative to the current baseline.
 * For example, downtimeReductionPercent = 50 means
 * "what if we cut downtime by 50%?"
 *
 * All fields default to 0 (no change).
 */
@Data
public class WhatIfRequest {

    /** Reduce downtime by this percentage (0-100) */
    private double downtimeReductionPercent = 0;

    /** Reduce processing time per unit by this percentage (0-100) */
    private double processingTimeReductionPercent = 0;

    /**
     * Add this much extra capacity (units/hour) to the bottleneck stage.
     * E.g., 10 = add a machine that can handle 10 more units/hour.
     */
    private double additionalCapacityUnitsPerHour = 0;

    /** Reduce the defect rate by this percentage (0-100) */
    private double defectRateReductionPercent = 0;
}
