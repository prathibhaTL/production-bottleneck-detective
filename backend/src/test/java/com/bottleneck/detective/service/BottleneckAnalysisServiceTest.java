package com.bottleneck.detective.service;

import com.bottleneck.detective.dto.StageMetrics;
import com.bottleneck.detective.entity.ProductionRecord;
import com.bottleneck.detective.entity.ProductionStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for the bottleneck scoring algorithm.
 *
 * These tests run WITHOUT Spring context (no database needed)
 * because we test the pure calculation methods directly.
 *
 * We extend BottleneckAnalysisService but pass null repositories –
 * the methods under test do not use them.
 */
class BottleneckAnalysisServiceTest {

    /**
     * We test computeStageMetrics() in isolation.
     * It only uses its two parameters – no repo calls.
     * We create the service with null repos because those paths aren't exercised.
     */
    private BottleneckAnalysisService service;

    @BeforeEach
    void setUp() {
        // Pass nulls – the tested method doesn't touch repositories
        service = new BottleneckAnalysisService(null, null, null, null);
    }

    // ── Helper: build a minimal ProductionStage ────────────────────────────
    private ProductionStage stage(double capacityPerHour, double stdProcessing) {
        ProductionStage s = new ProductionStage();
        s.setId(1L);
        s.setName("Test Stage");
        s.setCapacityPerHour(capacityPerHour);
        s.setStandardProcessingTimeMinutes(stdProcessing);
        return s;
    }

    // ── Helper: build a minimal ProductionRecord ───────────────────────────
    private ProductionRecord record(int units, int defects,
                                    double processing, double waiting,
                                    double downtime, double window) {
        ProductionStage s = new ProductionStage();
        s.setId(1L);
        s.setName("Test Stage");
        s.setCapacityPerHour(60);
        s.setStandardProcessingTimeMinutes(3);

        ProductionRecord r = new ProductionRecord();
        r.setStage(s);
        r.setUnitsProduced(units);
        r.setDefectiveUnits(defects);
        r.setProcessingTimeMinutes(processing);
        r.setWaitingTimeMinutes(waiting);
        r.setDowntimeMinutes(downtime);
        r.setTotalWindowMinutes(window);
        return r;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Tests
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Empty records list returns zero metrics and score 0")
    void emptyRecords_returnsZeroMetrics() {
        ProductionStage s = stage(60, 2);
        StageMetrics metrics = service.computeStageMetrics(s, List.of());

        assertThat(metrics.getCapacityUtilization()).isEqualTo(0.0);
        assertThat(metrics.getThroughput()).isEqualTo(0.0);
        assertThat(metrics.getBottleneckScore()).isEqualTo(0.0);
        assertThat(metrics.getContributingFactors()).isNotEmpty();
    }

    @Test
    @DisplayName("Perfect operation: no downtime, no defects, normal utilization → low score")
    void perfectOperation_producesLowBottleneckScore() {
        ProductionStage s = stage(60, 2.0); // 60 units/hour capacity

        // 1-hour window, 50 units, no defects, no downtime, minimal wait
        ProductionRecord r = record(50, 0, 2.0, 0.5, 0, 60);
        StageMetrics metrics = service.computeStageMetrics(s, List.of(r));

        assertThat(metrics.getCapacityUtilization()).isLessThan(90.0);
        assertThat(metrics.getDowntimePercentage()).isEqualTo(0.0);
        assertThat(metrics.getDefectRate()).isEqualTo(0.0);
        assertThat(metrics.getBottleneckScore()).isLessThan(50.0);
    }

    @Test
    @DisplayName("High downtime and waiting → high bottleneck score")
    void highDowntimeAndWaiting_producesHighScore() {
        ProductionStage s = stage(60, 3.0);

        // 1-hour window, only 20 units produced, 30 min downtime, 15 min wait
        ProductionRecord r = record(20, 0, 3.0, 15.0, 30.0, 60);
        StageMetrics metrics = service.computeStageMetrics(s, List.of(r));

        assertThat(metrics.getDowntimePercentage()).isGreaterThan(40.0);
        assertThat(metrics.getAvgWaitingTime()).isEqualTo(15.0);
        assertThat(metrics.getBottleneckScore()).isGreaterThan(40.0);
    }

    @Test
    @DisplayName("Defect rate is calculated correctly")
    void defectRateCalculation_isCorrect() {
        ProductionStage s = stage(60, 2);
        // 100 units, 10 defective → 10%
        ProductionRecord r = record(100, 10, 2.0, 1.0, 0, 60);
        StageMetrics metrics = service.computeStageMetrics(s, List.of(r));

        assertThat(metrics.getDefectRate()).isCloseTo(10.0, within(0.01));
    }

    @Test
    @DisplayName("Capacity utilization capped at 100%")
    void capacityUtilization_isCappedAt100() {
        // Stage can do 50/hour; window = 1 hour; we claim 200 units
        ProductionStage s = stage(50, 2);
        ProductionRecord r = record(200, 0, 2.0, 0, 0, 60);
        StageMetrics metrics = service.computeStageMetrics(s, List.of(r));

        assertThat(metrics.getCapacityUtilization()).isLessThanOrEqualTo(100.0);
    }

    @Test
    @DisplayName("Multiple records: metrics are aggregated across all records")
    void multipleRecords_metricsAreAggregated() {
        ProductionStage s = stage(60, 2);

        ProductionRecord r1 = record(30, 1, 2.0, 2.0, 5.0,  60);
        ProductionRecord r2 = record(28, 3, 3.0, 4.0, 10.0, 60);

        StageMetrics metrics = service.computeStageMetrics(s, List.of(r1, r2));

        // Total units = 58, total defects = 4 → defect rate ≈ 6.9%
        assertThat(metrics.getDefectRate()).isCloseTo(6.9, within(0.1));

        // Avg processing time = (2.0 + 3.0) / 2 = 2.5
        assertThat(metrics.getAvgProcessingTime()).isCloseTo(2.5, within(0.01));
    }

    @Test
    @DisplayName("Stage with problem metrics is scored higher than healthy stage")
    void problemStage_isHigherScoredThanHealthyStage() {
        ProductionStage healthy  = stage(60, 2);
        ProductionStage problem  = stage(60, 2);
        problem.setId(2L);

        ProductionRecord goodRecord    = record(55, 0, 2.0, 0.5, 0,    60);
        ProductionRecord badRecord     = record(20, 8, 10.0, 18.0, 30.0, 60);

        StageMetrics healthyMetrics = service.computeStageMetrics(healthy, List.of(goodRecord));
        StageMetrics problemMetrics = service.computeStageMetrics(problem, List.of(badRecord));

        assertThat(problemMetrics.getBottleneckScore())
                .isGreaterThan(healthyMetrics.getBottleneckScore());
    }

    @Test
    @DisplayName("Contributing factors list is non-empty for problem stage")
    void problemStage_hasContributingFactors() {
        ProductionStage s = stage(60, 3);
        ProductionRecord r = record(15, 10, 12.0, 20.0, 35.0, 60);

        StageMetrics metrics = service.computeStageMetrics(s, List.of(r));

        assertThat(metrics.getContributingFactors()).isNotEmpty();
        // At least one factor should mention downtime or waiting
        boolean hasMeaningfulFactor = metrics.getContributingFactors().stream()
                .anyMatch(f -> f.toLowerCase().contains("downtime")
                             || f.toLowerCase().contains("waiting")
                             || f.toLowerCase().contains("defect")
                             || f.toLowerCase().contains("processing"));
        assertThat(hasMeaningfulFactor).isTrue();
    }
}
