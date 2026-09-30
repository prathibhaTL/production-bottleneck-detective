package com.bottleneck.detective.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProductionRecordResponse {
    private Long id;
    private Long productionLineId;
    private String productionLineName;
    private Long stageId;
    private String stageName;
    private Long machineId;
    private String machineName;
    private LocalDateTime timestamp;
    private String shift;
    private int unitsProduced;
    private int defectiveUnits;
    private double processingTimeMinutes;
    private double waitingTimeMinutes;
    private double downtimeMinutes;
    private double totalWindowMinutes;
}
