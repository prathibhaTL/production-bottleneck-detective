package com.bottleneck.detective.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result returned by the factory simulator endpoint.
 * Tells the caller how many records were generated and what happened.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResult {

    private String message;
    private int recordsGenerated;
    private int downtimeEventsGenerated;
    private List<String> eventLog;   // short description of notable events
}
