package com.bottleneck.detective.controller;

import com.bottleneck.detective.dto.MachineRequest;
import com.bottleneck.detective.dto.MachineResponse;
import com.bottleneck.detective.service.MachineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/machines")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MachineController {

    private final MachineService machineService;

    @GetMapping
    public ResponseEntity<List<MachineResponse>> getAll() {
        return ResponseEntity.ok(machineService.getAllMachines());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MachineResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(machineService.getMachineById(id));
    }

    @PostMapping
    public ResponseEntity<MachineResponse> create(@Valid @RequestBody MachineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(machineService.createMachine(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MachineResponse> update(
            @PathVariable Long id, @Valid @RequestBody MachineRequest request) {
        return ResponseEntity.ok(machineService.updateMachine(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        machineService.deleteMachine(id);
        return ResponseEntity.noContent().build();
    }
}
