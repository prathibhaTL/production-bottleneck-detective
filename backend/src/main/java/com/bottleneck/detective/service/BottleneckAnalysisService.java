package com.bottleneck.detective.service;

import com.bottleneck.detective.dto.BottleneckResult;
import com.bottleneck.detective.dto.StageMetrics;
import com.bottleneck.detective.entity.*;
import com.bottleneck.detective.exception.ResourceNotFoundException;
import com.bottleneck.detective.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * BottleneckAnalysisService
 * ─────────────────────────
 * This is the core analytical engine of the application.
 *
 * HOW THE ALGORITHM WORKS
 * ───────────────────────
 * 1. Collect all ProductionRecords for the given line and time window.
 * 2. Group records by stage.
 * 3. For each stage compute six metrics:
 *      a) capacityUtilization  – how busy is this stage?
 *      b) throughput           – how much output is it delivering?
 *      c) avgProcessingTime    – how long does each unit take?
 *      d) avgWaitingTime       – how long do units wait before being processed?
 *      e) downtimePercentage   – how much of the window was the machine idle/broken?
 *      f) defectRate           – what fraction of output is rejected?
 * 4. Compute a weighted "bottleneck score" (0-100) for each stage.
 *    Weights are explained below each metric.
 * 5. The stage with the highest score is the detected bottleneck.
 * 6. Identify possible contributing factors (we avoid saying "the cause is…").
 * 7. Determine severity (LOW / MEDIUM / HIGH / CRITICAL) from the score.
 * 8. Persist the result to the bottleneck_analyses table.
 */
@Service
@RequiredArgsConstructor
public class BottleneckAnalysisService {

    private final ProductionRecordRepository recordRepository;
    private final ProductionLineRepository lineRepository;
    private final ProductionStageRepository stageRepository;
    private final BottleneckAnalysisRepository analysisRepository;

    // ── Scoring weights (must sum to 1.0) ──────────────────────────────────
    // Each weight reflects how strongly we expect that metric to indicate a bottleneck.
    // High downtime and high waiting time are the strongest signals.
    private static final double W_CAPACITY    = 0.20; // high utilization → overloaded
    private static final double W_DOWNTIME    = 0.25; // downtime directly reduces output
    private static final double W_WAITING     = 0.25; // queue buildup = upstream pushback
    private static final double W_PROCESSING  = 0.15; // slow cycle time slows the line
    private static final double W_DEFECT      = 0.15; // defects consume capacity for rework

    // ── Thresholds used to flag possible contributing factors ───────────────
    private static final double HIGH_UTILIZATION_THRESHOLD    = 85.0; // %
    private static final double HIGH_DOWNTIME_THRESHOLD       = 15.0; // %
    private static final double HIGH_WAITING_THRESHOLD        = 10.0; // minutes
    private static final double HIGH_PROCESSING_THRESHOLD     = 8.0;  // minutes per unit
    private static final double HIGH_DEFECT_THRESHOLD         = 5.0;  // %

    // ── Severity score ranges ───────────────────────────────────────────────
    private static final double SEV_LOW      = 30.0;
    private static final double SEV_MEDIUM   = 55.0;
    private static final double SEV_HIGH     = 75.0;

    // ───────────────────────────────────────────────────────────────────────
    // Public API
    // ───────────────────────────────────────────────────────────────────────

    /**
     * Run a full bottleneck analysis for the given production line using
     * records from the last {@code hoursBack} hours.
     *
     * @param lineId    ID of the production line to analyse
     * @param hoursBack how far back to look (default 24)
     */
    @Transactional
    public BottleneckResult analyzeBottleneck(Long lineId, int hoursBack) {
        ProductionLine line = lineRepository.findById(lineId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductionLine", lineId));

        LocalDateTime from = LocalDateTime.now().minusHours(hoursBack);
        LocalDateTime to   = LocalDateTime.now();

        List<ProductionRecord> records =
                recordRepository.findByProductionLineIdAndTimestampBetween(lineId, from, to);

        // Fall back to ALL records if none in the window (useful for demo data)
        if (records.isEmpty()) {
            records = recordRepository.findByProductionLineId(lineId);
        }

        List<ProductionStage> stages =
                stageRepository.findByProductionLineIdOrderByStageOrder(lineId);

        if (stages.isEmpty()) {
            return emptyResult(line);
        }

        // ── Step 1: Group records by stage ─────────────────────────────────
        Map<Long, List<ProductionRecord>> byStage = records.stream()
                .collect(Collectors.groupingBy(r -> r.getStage().getId()));

        // ── Step 2: Compute metrics per stage ──────────────────────────────
        List<StageMetrics> allMetrics = new ArrayList<>();
        for (ProductionStage stage : stages) {
            List<ProductionRecord> stageRecords = byStage.getOrDefault(stage.getId(), List.of());
            StageMetrics metrics = computeStageMetrics(stage, stageRecords);
            allMetrics.add(metrics);
        }

        // ── Step 3: Identify the bottleneck (highest score) ─────────────────
        StageMetrics bottleneck = allMetrics.stream()
                .max(Comparator.comparingDouble(StageMetrics::getBottleneckScore))
                .orElse(allMetrics.get(0));

        // ── Step 4: Sort all metrics by score descending for the frontend ───
        allMetrics.sort(Comparator.comparingDouble(StageMetrics::getBottleneckScore).reversed());

        // ── Step 5: Derive severity label ───────────────────────────────────
        String severityLabel = deriveSeverityLabel(bottleneck.getBottleneckScore());

        // ── Step 6: Build summary text ──────────────────────────────────────
        String summary = buildSummary(bottleneck, severityLabel, line.getName());

        // ── Step 7: Persist the analysis ────────────────────────────────────
        persistAnalysis(line, stages, bottleneck, severityLabel, summary);

        return BottleneckResult.builder()
                .productionLineId(line.getId())
                .productionLineName(line.getName())
                .bottleneckStage(bottleneck)
                .severityLabel(severityLabel)
                .severityScore(bottleneck.getBottleneckScore())
                .summary(summary)
                .allStageMetrics(allMetrics)
                .build();
    }

    // ───────────────────────────────────────────────────────────────────────
    // Core calculation
    // ───────────────────────────────────────────────────────────────────────

    /**
     * Computes all six metrics for one stage and derives a bottleneck score.
     */
    public StageMetrics computeStageMetrics(ProductionStage stage,
                                             List<ProductionRecord> records) {
        if (records.isEmpty()) {
            // Stage has no data – return neutral defaults
            return StageMetrics.builder()
                    .stageId(stage.getId())
                    .stageName(stage.getName())
                    .capacityUtilization(0)
                    .throughput(0)
                    .avgProcessingTime(0)
                    .avgWaitingTime(0)
                    .downtimePercentage(0)
                    .defectRate(0)
                    .bottleneckScore(0)
                    .contributingFactors(List.of("No production data available for this stage"))
                    .build();
        }

        // ── 1. Capacity utilization ─────────────────────────────────────────
        // Total units actually produced vs. theoretical maximum
        double totalUnits = records.stream().mapToInt(ProductionRecord::getUnitsProduced).sum();
        double totalWindowHours = records.stream()
                .mapToDouble(r -> r.getTotalWindowMinutes() / 60.0).sum();
        double theoreticalMax = stage.getCapacityPerHour() * totalWindowHours;
        double capacityUtilization = theoreticalMax > 0
                ? Math.min((totalUnits / theoreticalMax) * 100.0, 100.0) : 0.0;

        // ── 2. Throughput ───────────────────────────────────────────────────
        double throughput = totalWindowHours > 0 ? totalUnits / totalWindowHours : 0.0;

        // ── 3. Average processing time ──────────────────────────────────────
        double avgProcessingTime = records.stream()
                .mapToDouble(ProductionRecord::getProcessingTimeMinutes).average().orElse(0);

        // ── 4. Average waiting time ─────────────────────────────────────────
        double avgWaitingTime = records.stream()
                .mapToDouble(ProductionRecord::getWaitingTimeMinutes).average().orElse(0);

        // ── 5. Downtime percentage ──────────────────────────────────────────
        double totalDowntime = records.stream()
                .mapToDouble(ProductionRecord::getDowntimeMinutes).sum();
        double totalWindow = records.stream()
                .mapToDouble(ProductionRecord::getTotalWindowMinutes).sum();
        double downtimePercentage = totalWindow > 0
                ? (totalDowntime / totalWindow) * 100.0 : 0.0;

        // ── 6. Defect rate ──────────────────────────────────────────────────
        double totalDefects = records.stream()
                .mapToInt(ProductionRecord::getDefectiveUnits).sum();
        double defectRate = totalUnits > 0
                ? (totalDefects / totalUnits) * 100.0 : 0.0;

        // ── Scoring ─────────────────────────────────────────────────────────
        // Normalize each metric to a 0-100 scale before applying weights.
        // Cap at 100 so a single extreme value can't push score above 100.
        double normCapacity   = Math.min(capacityUtilization, 100.0);
        double normDowntime   = Math.min(downtimePercentage * 3, 100.0); // 33% downtime → 99 pts
        double normWaiting    = Math.min(avgWaitingTime * 5, 100.0);     // 20 min wait → 100 pts
        double normProcessing = Math.min(avgProcessingTime * 8, 100.0);  // 12.5 min/unit → 100 pts
        double normDefect     = Math.min(defectRate * 5, 100.0);         // 20% defect → 100 pts

        double score = (normCapacity   * W_CAPACITY)
                     + (normDowntime   * W_DOWNTIME)
                     + (normWaiting    * W_WAITING)
                     + (normProcessing * W_PROCESSING)
                     + (normDefect     * W_DEFECT);

        // ── Contributing factors ─────────────────────────────────────────────
        // Important: we say "possible contributing factor", not "confirmed cause".
        List<String> factors = identifyFactors(
                capacityUtilization, downtimePercentage,
                avgWaitingTime, avgProcessingTime, defectRate);

        return StageMetrics.builder()
                .stageId(stage.getId())
                .stageName(stage.getName())
                .capacityUtilization(round2(capacityUtilization))
                .throughput(round2(throughput))
                .avgProcessingTime(round2(avgProcessingTime))
                .avgWaitingTime(round2(avgWaitingTime))
                .downtimePercentage(round2(downtimePercentage))
                .defectRate(round2(defectRate))
                .bottleneckScore(round2(score))
                .contributingFactors(factors)
                .build();
    }

    // ───────────────────────────────────────────────────────────────────────
    // Helpers
    // ───────────────────────────────────────────────────────────────────────

    /**
     * Identifies possible contributing factors based on threshold comparisons.
     * Uses "possibly" / "may indicate" language to avoid overstating certainty.
     */
    private List<String> identifyFactors(double utilization, double downtime,
                                          double waiting, double processing, double defect) {
        List<String> factors = new ArrayList<>();

        if (utilization >= HIGH_UTILIZATION_THRESHOLD) {
            factors.add(String.format(
                    "High capacity utilization (%.1f%%) – this stage may be running near or at its limit", utilization));
        }
        if (downtime >= HIGH_DOWNTIME_THRESHOLD) {
            factors.add(String.format(
                    "Excessive downtime (%.1f%%) – machine outages may be reducing available capacity", downtime));
        }
        if (waiting >= HIGH_WAITING_THRESHOLD) {
            factors.add(String.format(
                    "High average waiting time (%.1f min) – possibly indicates queue buildup upstream of this stage", waiting));
        }
        if (processing >= HIGH_PROCESSING_THRESHOLD) {
            factors.add(String.format(
                    "High average processing time (%.1f min/unit) – cycle time may exceed the line's takt time", processing));
        }
        if (defect >= HIGH_DEFECT_THRESHOLD) {
            factors.add(String.format(
                    "Elevated defect rate (%.1f%%) – defects may be consuming capacity through rework or re-inspection", defect));
        }
        if (factors.isEmpty()) {
            factors.add("No significant anomalies detected – bottleneck is relative to other stages");
        }

        return factors;
    }

    private String deriveSeverityLabel(double score) {
        if (score >= SEV_HIGH)   return "CRITICAL";
        if (score >= SEV_MEDIUM) return "HIGH";
        if (score >= SEV_LOW)    return "MEDIUM";
        return "LOW";
    }

    private String buildSummary(StageMetrics b, String severity, String lineName) {
        return String.format(
                "Analysis of '%s': Stage '%s' has been identified as the primary bottleneck " +
                "(severity: %s, score: %.1f/100). " +
                "Key indicators: capacity utilization %.1f%%, downtime %.1f%%, " +
                "avg wait %.1f min, defect rate %.1f%%.",
                lineName, b.getStageName(), severity, b.getBottleneckScore(),
                b.getCapacityUtilization(), b.getDowntimePercentage(),
                b.getAvgWaitingTime(), b.getDefectRate());
    }

    private void persistAnalysis(ProductionLine line, List<ProductionStage> stages,
                                  StageMetrics bottleneck, String severityLabel, String summary) {
        BottleneckAnalysis analysis = new BottleneckAnalysis();
        analysis.setProductionLine(line);
        analysis.setAnalyzedAt(LocalDateTime.now());
        analysis.setCapacityUtilization(bottleneck.getCapacityUtilization());
        analysis.setThroughput(bottleneck.getThroughput());
        analysis.setAvgProcessingTime(bottleneck.getAvgProcessingTime());
        analysis.setAvgWaitingTime(bottleneck.getAvgWaitingTime());
        analysis.setDowntimePercentage(bottleneck.getDowntimePercentage());
        analysis.setDefectRate(bottleneck.getDefectRate());
        analysis.setSeverityScore(bottleneck.getBottleneckScore());
        analysis.setSeverityLabel(severityLabel);
        analysis.setSummary(summary);
        analysis.setContributingFactors(
                bottleneck.getContributingFactors().toString());

        // Link to the actual stage entity
        stages.stream()
                .filter(s -> s.getId().equals(bottleneck.getStageId()))
                .findFirst()
                .ifPresent(analysis::setBottleneckStage);

        analysisRepository.save(analysis);
    }

    private BottleneckResult emptyResult(ProductionLine line) {
        return BottleneckResult.builder()
                .productionLineId(line.getId())
                .productionLineName(line.getName())
                .summary("No stages found for this production line.")
                .allStageMetrics(List.of())
                .build();
    }

    /** Rounds a double to 2 decimal places */
    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
