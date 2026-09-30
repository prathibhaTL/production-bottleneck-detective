package com.bottleneck.detective.dto;

import lombok.Data;

@Data
public class ProductionStageResponse {
    private Long id;
    private String name;
    private int stageOrder;
    private double capacityPerHour;
    private double standardProcessingTimeMinutes;
    private String status;
    private Long productionLineId;
    private String productionLineName;
    private Long machineId;
    private String machineName;
}
