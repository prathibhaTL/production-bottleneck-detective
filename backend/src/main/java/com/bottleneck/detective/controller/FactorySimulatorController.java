package com.bottleneck.detective.controller;

import com.bottleneck.detective.dto.SimulationResult;
import com.bottleneck.detective.service.FactorySimulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulator")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class FactorySimulatorController {

    private final FactorySimulatorService simulatorService;

    /**
     * POST /api/simulator/run/{lineId}?periods=24
     *
     * Generates synthetic production records to demonstrate the
     * bottleneck detection engine. Optionally specify how many
     * 1-hour periods to simulate (default 24).
     */
    @PostMapping("/run/{lineId}")
    public ResponseEntity<SimulationResult> run(
            @PathVariable Long lineId,
            @RequestParam(defaultValue = "24") int periods) {
        return ResponseEntity.ok(simulatorService.runSimulation(lineId, periods));
    }
}
