package com.bottleneck.detective.controller;

import com.bottleneck.detective.dto.BottleneckResult;
import com.bottleneck.detective.service.BottleneckAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for triggering and retrieving bottleneck analysis.
 */
@RestController
@RequestMapping("/api/analysis")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class BottleneckAnalysisController {

    private final BottleneckAnalysisService analysisService;

    /**
     * POST /api/analysis/bottleneck/{lineId}
     * Runs the analysis engine on the given production line.
     *
     * @param lineId    which production line to analyse
     * @param hoursBack how many hours back to look at records (default 24)
     */
    @PostMapping("/bottleneck/{lineId}")
    public ResponseEntity<BottleneckResult> analyze(
            @PathVariable Long lineId,
            @RequestParam(defaultValue = "24") int hoursBack) {
        return ResponseEntity.ok(analysisService.analyzeBottleneck(lineId, hoursBack));
    }

    /**
     * GET /api/analysis/bottleneck/{lineId}
     * Convenience GET endpoint – same analysis, no side effects for the caller.
     */
    @GetMapping("/bottleneck/{lineId}")
    public ResponseEntity<BottleneckResult> getAnalysis(
            @PathVariable Long lineId,
            @RequestParam(defaultValue = "24") int hoursBack) {
        return ResponseEntity.ok(analysisService.analyzeBottleneck(lineId, hoursBack));
    }
}
