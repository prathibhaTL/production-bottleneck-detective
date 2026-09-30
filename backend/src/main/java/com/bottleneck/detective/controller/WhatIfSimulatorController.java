package com.bottleneck.detective.controller;

import com.bottleneck.detective.dto.WhatIfRequest;
import com.bottleneck.detective.dto.WhatIfResult;
import com.bottleneck.detective.service.WhatIfSimulatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulation")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class WhatIfSimulatorController {

    private final WhatIfSimulatorService whatIfSimulatorService;

    /**
     * POST /api/simulation/what-if/{lineId}
     *
     * Body example:
     * {
     *   "downtimeReductionPercent": 40,
     *   "processingTimeReductionPercent": 20,
     *   "additionalCapacityUnitsPerHour": 5,
     *   "defectRateReductionPercent": 30
     * }
     */
    @PostMapping("/what-if/{lineId}")
    public ResponseEntity<WhatIfResult> runWhatIf(
            @PathVariable Long lineId,
            @RequestBody WhatIfRequest request) {
        return ResponseEntity.ok(whatIfSimulatorService.simulate(lineId, request));
    }
}
