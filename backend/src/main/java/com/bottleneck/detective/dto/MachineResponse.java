package com.bottleneck.detective.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MachineResponse {
    private Long id;
    private String name;
    private String type;
    private String modelNumber;
    private String status;
    private LocalDateTime installedAt;
}
