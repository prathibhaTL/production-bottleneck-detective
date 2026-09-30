package com.bottleneck.detective.service;

import com.bottleneck.detective.dto.ProductionStageRequest;
import com.bottleneck.detective.dto.ProductionStageResponse;
import com.bottleneck.detective.entity.Machine;
import com.bottleneck.detective.entity.ProductionLine;
import com.bottleneck.detective.entity.ProductionStage;
import com.bottleneck.detective.exception.ResourceNotFoundException;
import com.bottleneck.detective.repository.MachineRepository;
import com.bottleneck.detective.repository.ProductionLineRepository;
import com.bottleneck.detective.repository.ProductionStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionStageService {

    private final ProductionStageRepository stageRepository;
    private final ProductionLineRepository lineRepository;
    private final MachineRepository machineRepository;

    public List<ProductionStageResponse> getStagesByLine(Long lineId) {
        return stageRepository.findByProductionLineIdOrderByStageOrder(lineId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ProductionStageResponse getStageById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ProductionStageResponse createStage(ProductionStageRequest request) {
        ProductionLine line = lineRepository.findById(request.getProductionLineId())
                .orElseThrow(() -> new ResourceNotFoundException("ProductionLine", request.getProductionLineId()));

        ProductionStage stage = new ProductionStage();
        stage.setName(request.getName());
        stage.setStageOrder(request.getStageOrder());
        stage.setCapacityPerHour(request.getCapacityPerHour());
        stage.setStandardProcessingTimeMinutes(request.getStandardProcessingTimeMinutes());
        stage.setStatus(request.getStatus() != null ? request.getStatus() : "Active");
        stage.setProductionLine(line);

        if (request.getMachineId() != null) {
            Machine machine = machineRepository.findById(request.getMachineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Machine", request.getMachineId()));
            stage.setMachine(machine);
        }

        return toResponse(stageRepository.save(stage));
    }

    @Transactional
    public ProductionStageResponse updateStage(Long id, ProductionStageRequest request) {
        ProductionStage stage = findOrThrow(id);
        stage.setName(request.getName());
        stage.setStageOrder(request.getStageOrder());
        stage.setCapacityPerHour(request.getCapacityPerHour());
        stage.setStandardProcessingTimeMinutes(request.getStandardProcessingTimeMinutes());
        if (request.getStatus() != null) stage.setStatus(request.getStatus());

        if (request.getMachineId() != null) {
            Machine machine = machineRepository.findById(request.getMachineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Machine", request.getMachineId()));
            stage.setMachine(machine);
        }

        return toResponse(stageRepository.save(stage));
    }

    @Transactional
    public void deleteStage(Long id) {
        stageRepository.delete(findOrThrow(id));
    }

    private ProductionStage findOrThrow(Long id) {
        return stageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductionStage", id));
    }

    public ProductionStageResponse toResponse(ProductionStage s) {
        ProductionStageResponse r = new ProductionStageResponse();
        r.setId(s.getId());
        r.setName(s.getName());
        r.setStageOrder(s.getStageOrder());
        r.setCapacityPerHour(s.getCapacityPerHour());
        r.setStandardProcessingTimeMinutes(s.getStandardProcessingTimeMinutes());
        r.setStatus(s.getStatus());
        r.setProductionLineId(s.getProductionLine().getId());
        r.setProductionLineName(s.getProductionLine().getName());
        if (s.getMachine() != null) {
            r.setMachineId(s.getMachine().getId());
            r.setMachineName(s.getMachine().getName());
        }
        return r;
    }

    public ProductionStage findEntityById(Long id) {
        return findOrThrow(id);
    }
}
