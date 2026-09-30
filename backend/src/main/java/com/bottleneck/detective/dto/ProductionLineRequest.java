package com.bottleneck.detective.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Payload sent by the client when creating or updating a ProductionLine */
@Data
public class ProductionLineRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @Size(max = 255)
    private String description;

    private String status = "Active";
}
