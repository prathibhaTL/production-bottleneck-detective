package com.bottleneck.detective.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MachineRequest {

    @NotBlank(message = "Machine name is required")
    private String name;

    private String type;
    private String modelNumber;
    private String status = "Running";
}
