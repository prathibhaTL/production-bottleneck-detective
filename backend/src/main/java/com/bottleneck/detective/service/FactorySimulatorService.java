package com.bottleneck.detective.service;

import com.bottleneck.detective.dto.SimulationResult;
import com.bottleneck.detective.entity.*;
import com.bottleneck.detective.exception.ResourceNotFoundException;
import com.bottleneck.detective.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * FactorySimulatorService
 * ───────────────────────
 * Generates realistic (but synthetic) production records for a given
 * production line so that the bottleneck engine has data to work with.
 *
 * HOW EVENTS ARE GENERATED
 * ─────────────────────────
 * 1. For each stage, generate {@code periodsToSimulate} records spaced 1 hour apart.
 * 2. Base units produced = stage.capacityPerHour * windowHours, with ±10% noise.
 * 3. Randomly inject adverse events:
 *      • Machine downtime   (15% probability per record)
 *      • High processing time (10% probability)
 *      • Queue buildup      (20% probability)
 *      • Defective units    (always present, sometimes spiked)
 * 4. One stage is designated the "problem stage" for this run – it gets
 *    significantly worse metrics so the bottleneck engine can detect it.
 *
 * NOTE: This service writes directly to the database.
 * For a real system you would separate demo data from production data.
 */
@Service
@RequiredArgsConstructor
public class FactorySimulatorService {

    private final ProductionLineRepository lineRepository;
    private final ProductionStageRepository stageRepository;
    private final ProductionRecordRepository recordRepository;
    private final DowntimeEventRepository downtimeEventRepository;

    private final Random random = new Random();

    /**
     * Generates simulated production records for the given production line.
     *
     * @param lineId           which line to simulate
     * @param periodsToSimulate how many hourly time periods to generate (default 24)
     */
    @Transactional
    public SimulationResult runSimulation(Long lineId, int periodsToSimulate) {

        ProductionLine line = lineRepository.findById(lineId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductionLine", lineId));

        List<ProductionStage> stages =
                stageRepository.findByProductionLineIdOrderByStageOrder(lineId);

        if (stages.isEmpty()) {
            return SimulationResult.builder()
                    .message("No stages found for this production line. Add stages first.")
                    .recordsGenerated(0)
                    .build();
        }

        // Pick one stage to be the "problem stage" for this simulation run
        int problemStageIndex = random.nextInt(stages.size());
        ProductionStage problemStage = stages.get(problemStageIndex);

        List<String> eventLog = new ArrayList<>();
        eventLog.add("Simulation started for line: " + line.getName());
        eventLog.add("Problem stage injected: " + problemStage.getName());

        int recordCount = 0;
        int downtimeCount = 0;

        LocalDateTime now = LocalDateTime.now();

        for (ProductionStage stage : stages) {
            boolean isProblemStage = stage.getId().equals(problemStage.getId());

            for (int period = periodsToSimulate; period >= 1; period--) {
                LocalDateTime timestamp = now.minusHours(period);
                String shift = determineShift(timestamp.getHour());

                // ── Base production ────────────────────────────────────────
                double windowMinutes = 60.0; // 1-hour windows
                double windowHours   = windowMinutes / 60.0;

                // Introduce ±10% random noise
                double noise = 0.9 + (random.nextDouble() * 0.2);
                double baseUnits = stage.getCapacityPerHour() * windowHours * noise;

                // ── Randomised events ──────────────────────────────────────
                double downtimeMinutes   = 0;
                double waitingExtra      = 0;
                double processingExtra   = 0;
                double defectMultiplier  = 1.0;

                // Problem stage gets worse metrics on purpose
                if (isProblemStage) {
                    // Always has elevated downtime
                    downtimeMinutes  = 10 + random.nextDouble() * 25;  // 10-35 min
                    waitingExtra     = 8  + random.nextDouble() * 12;  // 8-20 extra min wait
                    processingExtra  = 2  + random.nextDouble() * 6;   // 2-8 extra min/unit
                    defectMultiplier = 1.5 + random.nextDouble();      // 1.5-2.5x defects
                    eventLog.add(String.format("[Period -%dh] %s: downtime=%.0fmin, extra_wait=%.0fmin",
                            period, stage.getName(), downtimeMinutes, waitingExtra));
                } else {
                    // Normal stages occasionally have events
                    if (random.nextDouble() < 0.15) {
                        downtimeMinutes = 2 + random.nextDouble() * 15; // 2-17 min
                        eventLog.add(String.format("[Period -%dh] %s: minor downtime %.0f min",
                                period, stage.getName(), downtimeMinutes));
                    }
                    if (random.nextDouble() < 0.10) {
                        processingExtra = 1 + random.nextDouble() * 3;
                    }
                    if (random.nextDouble() < 0.20) {
                        waitingExtra = 1 + random.nextDouble() * 5;
                    }
                }

                // Reduce output proportionally to downtime
                double effectiveMinutes = windowMinutes - downtimeMinutes;
                double actualUnits = Math.max(0,
                        (effectiveMinutes / windowMinutes) * baseUnits);

                // Defective units (base ~2-3%, problem stage higher)
                double baseDefectRate = 0.02 + random.nextDouble() * 0.01;
                int defectiveUnits = (int) Math.round(actualUnits * baseDefectRate * defectMultiplier);
                defectiveUnits = Math.min(defectiveUnits, (int) actualUnits);

                // ── Build and save ProductionRecord ────────────────────────
                ProductionRecord record = new ProductionRecord();
                record.setProductionLine(line);
                record.setStage(stage);
                record.setMachine(stage.getMachine());
                record.setTimestamp(timestamp);
                record.setShift(shift);
                record.setUnitsProduced((int) Math.round(actualUnits));
                record.setDefectiveUnits(defectiveUnits);
                record.setProcessingTimeMinutes(
                        stage.getStandardProcessingTimeMinutes() + processingExtra);
                record.setWaitingTimeMinutes(
                        (isProblemStage ? 5.0 : 1.0) + waitingExtra);
                record.setDowntimeMinutes(downtimeMinutes);
                record.setTotalWindowMinutes(windowMinutes);

                recordRepository.save(record);
                recordCount++;

                // ── Persist downtime event if downtime occurred ────────────
                if (downtimeMinutes > 5 && stage.getMachine() != null) {
                    DowntimeEvent event = new DowntimeEvent();
                    event.setMachine(stage.getMachine());
                    event.setStage(stage);
                    event.setStartTime(timestamp);
                    event.setEndTime(timestamp.plusMinutes((long) downtimeMinutes));
                    event.setDurationMinutes(downtimeMinutes);
                    event.setReason(isProblemStage
                            ? "Simulated recurring fault – possible mechanical degradation"
                            : "Simulated minor interruption");
                    event.setSeverity(isProblemStage ? "HIGH" : "LOW");
                    downtimeEventRepository.save(event);
                    downtimeCount++;
                }
            }
        }

        eventLog.add("Simulation complete. Records: " + recordCount
                + ", Downtime events: " + downtimeCount);

        return SimulationResult.builder()
                .message("Simulation completed successfully for line: " + line.getName())
                .recordsGenerated(recordCount)
                .downtimeEventsGenerated(downtimeCount)
                .eventLog(eventLog)
                .build();
    }

    private String determineShift(int hour) {
        if (hour >= 6  && hour < 14) return "Morning";
        if (hour >= 14 && hour < 22) return "Afternoon";
        return "Night";
    }
}
