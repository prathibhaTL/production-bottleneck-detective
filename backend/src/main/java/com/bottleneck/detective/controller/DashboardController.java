package com.bottleneck.detective.controller;

import com.bottleneck.detective.dto.DashboardSummary;
import com.bottleneck.detective.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /** GET /api/dashboard/{lineId} → full dashboard summary for a production line */
    @GetMapping("/{lineId}")
    public ResponseEntity<DashboardSummary> getSummary(@PathVariable Long lineId) {
        return ResponseEntity.ok(dashboardService.getSummary(lineId));
    }
}
