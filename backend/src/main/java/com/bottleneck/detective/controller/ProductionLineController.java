package com.bottleneck.detective.controller;

import com.bottleneck.detective.dto.ProductionLineRequest;
import com.bottleneck.detective.dto.ProductionLineResponse;
import com.bottleneck.detective.service.ProductionLineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Production Lines.
 *
 * @RestController = @Controller + @ResponseBody → all methods return JSON
 * @RequestMapping sets the URL prefix for all endpoints in this class
 * @CrossOrigin allows the React frontend (running on a different port) to call this API
 */
@RestController
@RequestMapping("/api/production-lines")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProductionLineController {

    private final ProductionLineService productionLineService;

    /** GET /api/production-lines → list all production lines */
    @GetMapping
    public ResponseEntity<List<ProductionLineResponse>> getAll() {
        return ResponseEntity.ok(productionLineService.getAllLines());
    }

    /** GET /api/production-lines/{id} → get one production line */
    @GetMapping("/{id}")
    public ResponseEntity<ProductionLineResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productionLineService.getLineById(id));
    }

    /** POST /api/production-lines → create a new production line */
    @PostMapping
    public ResponseEntity<ProductionLineResponse> create(
            @Valid @RequestBody ProductionLineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productionLineService.createLine(request));
    }

    /** PUT /api/production-lines/{id} → update an existing production line */
    @PutMapping("/{id}")
    public ResponseEntity<ProductionLineResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductionLineRequest request) {
        return ResponseEntity.ok(productionLineService.updateLine(id, request));
    }

    /** DELETE /api/production-lines/{id} → delete a production line */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productionLineService.deleteLine(id);
        return ResponseEntity.noContent().build();
    }
}
