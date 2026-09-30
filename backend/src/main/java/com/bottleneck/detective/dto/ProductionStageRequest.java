package com.bottleneck.detective.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ProductionStageRequest {

    @NotBlank(message = "Stage name is required")
    private String name;

    private int stageOrder = 1;

    @Positive(message = "Capacity per hour must be positive")
    private double capacityPerHour;

    @Positive(message = "Standard processing time must be positive")
    private double standardProcessingTimeMinutes;

    private String status = "Active";

    @NotNull(message = "Production line ID is required")
    private Long productionLineId;

    private Long machineId; // optional
}
