package com.bottleneck.detective.service;

import com.bottleneck.detective.dto.BottleneckResult;
import com.bottleneck.detective.dto.StageMetrics;
import com.bottleneck.detective.dto.WhatIfRequest;
import com.bottleneck.detective.dto.WhatIfResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * WhatIfSimulatorService
 * ──────────────────────
 * Takes the CURRENT bottleneck stage metrics and a set of hypothetical
 * changes (e.g., "reduce downtime by 40%") and estimates the new throughput.
 *
 * ─── Estimation model ───────────────────────────────────────────────────
 *
 * 1. DOWNTIME REDUCTION
 *    Extra available minutes = downtimeMinutes * (reductionPercent / 100)
 *    Those minutes can produce: extraMinutes / processingTimePerUnit units.
 *    Throughput gain = extraUnits / windowHours
 *
 * 2. PROCESSING TIME REDUCTION
 *    If we shorten cycle time, we can process more units in the same window.
 *    New throughput = oldThroughput / (1 - reductionPercent/100)
 *    Capped at stage capacity.
 *
 * 3. ADDITIONAL CAPACITY
 *    Directly added to throughput, capped at total demand.
 *
 * 4. DEFECT RATE REDUCTION
 *    Fewer defects → more good units produced.
 *    If defect rate drops, effective throughput = throughput * (1 - newDefectRate/100)
 *
 * These are LINEAR approximations – clearly an oversimplification of
 * real factory dynamics. The disclaimer in WhatIfResult makes this explicit.
 */
@Service
@RequiredArgsConstructor
public class WhatIfSimulatorService {

    private final BottleneckAnalysisService analysisService;

    /**
     * Run the what-if simulation for the current bottleneck of the given line.
     *
     * @param lineId  the production line
     * @param request the what-if parameters
     */
    public WhatIfResult simulate(Long lineId, WhatIfRequest request) {

        // Step 1 – get current analysis to extract the bottleneck stage metrics
        BottleneckResult currentAnalysis = analysisService.analyzeBottleneck(lineId, 24);
        StageMetrics baseline = currentAnalysis.getBottleneckStage();

        if (baseline == null) {
            return WhatIfResult.builder()
                    .disclaimer("No bottleneck stage found. Please run an analysis first.")
                    .build();
        }

        // ── Baseline values ────────────────────────────────────────────────
        double baselineThroughput    = baseline.getThroughput();          // units/hour
        double baselineDowntime      = baseline.getDowntimePercentage();  // %
        double baselineProcessing    = baseline.getAvgProcessingTime();   // min/unit
        double baselineDefectRate    = baseline.getDefectRate();          // %

        // ── Apply each improvement in sequence ─────────────────────────────
        double estimatedThroughput = baselineThroughput;

        // 1. Downtime reduction
        // Interpretation: if 20% of time was downtime and we cut that by 50%,
        // we recover 10% of window time → more units produced per hour.
        if (request.getDowntimeReductionPercent() > 0 && baselineProcessing > 0) {
            double downtimeAsHourFraction = baselineDowntime / 100.0;
            double recoveredHourFraction  = downtimeAsHourFraction
                                            * (request.getDowntimeReductionPercent() / 100.0);
            // Each recovered hour can produce (60 / processingTime) units
            double extraUnitsPerHour = recoveredHourFraction * (60.0 / baselineProcessing);
            estimatedThroughput += extraUnitsPerHour;
        }

        // 2. Processing time reduction
        // Faster cycle → more throughput, proportionally
        if (request.getProcessingTimeReductionPercent() > 0) {
            double reductionFactor = 1.0 - (request.getProcessingTimeReductionPercent() / 100.0);
            if (reductionFactor > 0) {
                estimatedThroughput = estimatedThroughput / reductionFactor;
            }
        }

        // 3. Additional capacity
        estimatedThroughput += request.getAdditionalCapacityUnitsPerHour();

        // 4. Defect rate improvement
        // Fewer defects → higher effective yield
        double newDefectRate = Math.max(0, baselineDefectRate
                - (baselineDefectRate * (request.getDefectRateReductionPercent() / 100.0)));
        double yieldBefore = 1.0 - (baselineDefectRate / 100.0);
        double yieldAfter  = 1.0 - (newDefectRate / 100.0);
        if (yieldBefore > 0) {
            estimatedThroughput = estimatedThroughput * (yieldAfter / yieldBefore);
        }

        // ── Derived estimated metrics ───────────────────────────────────────
        double estimatedDowntime   = Math.max(0, baselineDowntime
                - baselineDowntime * (request.getDowntimeReductionPercent() / 100.0));
        double estimatedProcessing = Math.max(0, baselineProcessing
                - baselineProcessing * (request.getProcessingTimeReductionPercent() / 100.0));

        double improvement = estimatedThroughput - baselineThroughput;
        double improvementPct = baselineThroughput > 0
                ? (improvement / baselineThroughput) * 100.0 : 0.0;

        return WhatIfResult.builder()
                .targetStageName(baseline.getStageName())
                .downtimeReductionPercent(request.getDowntimeReductionPercent())
                .processingTimeReductionPercent(request.getProcessingTimeReductionPercent())
                .additionalCapacityUnitsPerHour(request.getAdditionalCapacityUnitsPerHour())
                .defectRateReductionPercent(request.getDefectRateReductionPercent())
                .baselineThroughput(round2(baselineThroughput))
                .baselineDowntimePercent(round2(baselineDowntime))
                .baselineProcessingTime(round2(baselineProcessing))
                .baselineDefectRate(round2(baselineDefectRate))
                .estimatedThroughput(round2(estimatedThroughput))
                .estimatedDowntimePercent(round2(estimatedDowntime))
                .estimatedProcessingTime(round2(estimatedProcessing))
                .estimatedDefectRate(round2(newDefectRate))
                .throughputImprovementUnitsPerHour(round2(improvement))
                .throughputImprovementPercent(round2(improvementPct))
                .build();
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
