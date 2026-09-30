package com.bottleneck.detective.controller;

import com.bottleneck.detective.dto.ProductionRecordRequest;
import com.bottleneck.detective.dto.ProductionRecordResponse;
import com.bottleneck.detective.service.ProductionRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/records")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProductionRecordController {

    private final ProductionRecordService recordService;

    /** GET /api/records?lineId=1 OR ?stageId=2 */
    @GetMapping
    public ResponseEntity<List<ProductionRecordResponse>> getRecords(
            @RequestParam(required = false) Long lineId,
            @RequestParam(required = false) Long stageId) {
        if (stageId != null) {
            return ResponseEntity.ok(recordService.getRecordsByStage(stageId));
        }
        if (lineId != null) {
            return ResponseEntity.ok(recordService.getRecordsByLine(lineId));
        }
        // Return empty list if neither parameter is given (avoid full table scan)
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductionRecordResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(recordService.getRecordById(id));
    }

    @PostMapping
    public ResponseEntity<ProductionRecordResponse> create(
            @Valid @RequestBody ProductionRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recordService.createRecord(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        recordService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }
}
