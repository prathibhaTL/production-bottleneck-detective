package com.bottleneck.detective.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductionRecordRequest {

    @NotNull private Long productionLineId;
    @NotNull private Long stageId;
    private Long machineId;

    private LocalDateTime timestamp;
    private String shift;

    @Min(0) private int unitsProduced;
    @Min(0) private int defectiveUnits;
    @Min(0) private double processingTimeMinutes;
    @Min(0) private double waitingTimeMinutes;
    @Min(0) private double downtimeMinutes;
    private double totalWindowMinutes = 480.0;
}
