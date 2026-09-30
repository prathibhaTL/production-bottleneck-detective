package com.bottleneck.detective.dto;

import lombok.Data;
import java.time.LocalDateTime;

/** Data sent back to the client when returning a ProductionLine */
@Data
public class ProductionLineResponse {
    private Long id;
    private String name;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private int stageCount;
}
