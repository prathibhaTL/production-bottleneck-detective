package com.bottleneck.detective.controller;

import com.bottleneck.detective.dto.ProductionStageRequest;
import com.bottleneck.detective.dto.ProductionStageResponse;
import com.bottleneck.detective.service.ProductionStageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stages")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProductionStageController {

    private final ProductionStageService stageService;

    /** GET /api/stages?lineId=1 → list stages for a production line */
    @GetMapping
    public ResponseEntity<List<ProductionStageResponse>> getByLine(
            @RequestParam Long lineId) {
        return ResponseEntity.ok(stageService.getStagesByLine(lineId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductionStageResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(stageService.getStageById(id));
    }

    @PostMapping
    public ResponseEntity<ProductionStageResponse> create(
            @Valid @RequestBody ProductionStageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(stageService.createStage(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductionStageResponse> update(
            @PathVariable Long id, @Valid @RequestBody ProductionStageRequest request) {
        return ResponseEntity.ok(stageService.updateStage(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        stageService.deleteStage(id);
        return ResponseEntity.noContent().build();
    }
}
