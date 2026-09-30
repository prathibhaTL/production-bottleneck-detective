package com.bottleneck.detective.service;

import com.bottleneck.detective.dto.BottleneckResult;
import com.bottleneck.detective.dto.DashboardSummary;
import com.bottleneck.detective.entity.ProductionLine;
import com.bottleneck.detective.entity.ProductionRecord;
import com.bottleneck.detective.entity.ProductionStage;
import com.bottleneck.detective.exception.ResourceNotFoundException;
import com.bottleneck.detective.repository.ProductionLineRepository;
import com.bottleneck.detective.repository.ProductionRecordRepository;
import com.bottleneck.detective.repository.ProductionStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductionLineRepository lineRepository;
    private final ProductionRecordRepository recordRepository;
    private final ProductionStageRepository stageRepository;
    private final BottleneckAnalysisService bottleneckAnalysisService;

    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    public DashboardSummary getSummary(Long lineId) {
        ProductionLine line = lineRepository.findById(lineId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductionLine", lineId));

        List<ProductionRecord> records = recordRepository.findByProductionLineId(lineId);
        List<ProductionStage> stages = stageRepository.findByProductionLineIdOrderByStageOrder(lineId);

        // ── Totals ─────────────────────────────────────────────────────────
        int totalUnits    = records.stream().mapToInt(ProductionRecord::getUnitsProduced).sum();
        int totalDefects  = records.stream().mapToInt(ProductionRecord::getDefectiveUnits).sum();
        double defectRate = totalUnits > 0 ? (totalDefects * 100.0 / totalUnits) : 0;

        double totalDowntime = records.stream().mapToDouble(ProductionRecord::getDowntimeMinutes).sum();
        double totalWindow   = records.stream().mapToDouble(ProductionRecord::getTotalWindowMinutes).sum();
        double downtimePct   = totalWindow > 0 ? (totalDowntime / totalWindow) * 100 : 0;

        // ── Production efficiency ──────────────────────────────────────────
        // = (good units / theoretical max) * 100
        double totalWindowHours = totalWindow / 60.0;
        double theoreticalMax = stages.stream()
                .mapToDouble(s -> s.getCapacityPerHour() * totalWindowHours / stages.size())
                .sum();
        double efficiency = theoreticalMax > 0
                ? Math.min(((totalUnits - totalDefects) / theoreticalMax) * 100, 100) : 0;

        // ── Stage utilization & queue map ──────────────────────────────────
        Map<Long, List<ProductionRecord>> byStage = records.stream()
                .collect(Collectors.groupingBy(r -> r.getStage().getId()));

        Map<String, Double> utilMap = new LinkedHashMap<>();
        Map<String, Double> queueMap = new LinkedHashMap<>();

        for (ProductionStage stage : stages) {
            List<ProductionRecord> sr = byStage.getOrDefault(stage.getId(), List.of());
            double stageUnits = sr.stream().mapToInt(ProductionRecord::getUnitsProduced).sum();
            double stageWindowH = sr.stream().mapToDouble(r -> r.getTotalWindowMinutes() / 60.0).sum();
            double cap = stage.getCapacityPerHour() * stageWindowH;
            double util = cap > 0 ? Math.min((stageUnits / cap) * 100, 100) : 0;
            double avgWait = sr.stream().mapToDouble(ProductionRecord::getWaitingTimeMinutes)
                    .average().orElse(0);
            utilMap.put(stage.getName(), Math.round(util * 10) / 10.0);
            queueMap.put(stage.getName(), Math.round(avgWait * 10) / 10.0);
        }

        // ── Trend (last 24 records, sorted oldest first) ───────────────────
        List<ProductionRecord> recent =
                recordRepository.findTop50ByProductionLineIdOrderByTimestampDesc(lineId);
        Collections.reverse(recent);

        Map<String, Integer> trendMap = new LinkedHashMap<>();
        for (ProductionRecord r : recent) {
            String key = r.getTimestamp().format(HOUR_FMT);
            trendMap.merge(key, r.getUnitsProduced(), Integer::sum);
        }
        List<DashboardSummary.TrendPoint> trend = trendMap.entrySet().stream()
                .map(e -> new DashboardSummary.TrendPoint(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // ── Bottleneck ─────────────────────────────────────────────────────
        String bottleneckStage = "N/A";
        String bottleneckSeverity = "N/A";
        try {
            BottleneckResult analysis = bottleneckAnalysisService.analyzeBottleneck(lineId, 48);
            if (analysis.getBottleneckStage() != null) {
                bottleneckStage    = analysis.getBottleneckStage().getStageName();
                bottleneckSeverity = analysis.getSeverityLabel();
            }
        } catch (Exception ignored) { /* return N/A if analysis fails */ }

        return DashboardSummary.builder()
                .productionLineId(lineId)
                .productionLineName(line.getName())
                .totalUnitsProduced(totalUnits)
                .totalDefectiveUnits(totalDefects)
                .overallDefectRate(Math.round(defectRate * 10) / 10.0)
                .overallDowntimePercent(Math.round(downtimePct * 10) / 10.0)
                .productionEfficiency(Math.round(efficiency * 10) / 10.0)
                .currentBottleneckStage(bottleneckStage)
                .bottleneckSeverity(bottleneckSeverity)
                .stageUtilization(utilMap)
                .stageQueueTime(queueMap)
                .productionTrend(trend)
                .build();
    }
}
