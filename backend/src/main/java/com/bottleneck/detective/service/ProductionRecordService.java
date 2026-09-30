package com.bottleneck.detective.service;

import com.bottleneck.detective.dto.ProductionRecordRequest;
import com.bottleneck.detective.dto.ProductionRecordResponse;
import com.bottleneck.detective.entity.Machine;
import com.bottleneck.detective.entity.ProductionLine;
import com.bottleneck.detective.entity.ProductionRecord;
import com.bottleneck.detective.entity.ProductionStage;
import com.bottleneck.detective.exception.ResourceNotFoundException;
import com.bottleneck.detective.repository.MachineRepository;
import com.bottleneck.detective.repository.ProductionLineRepository;
import com.bottleneck.detective.repository.ProductionRecordRepository;
import com.bottleneck.detective.repository.ProductionStageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductionRecordService {

    private final ProductionRecordRepository recordRepository;
    private final ProductionLineRepository lineRepository;
    private final ProductionStageRepository stageRepository;
    private final MachineRepository machineRepository;

    public List<ProductionRecordResponse> getRecordsByLine(Long lineId) {
        return recordRepository.findByProductionLineId(lineId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ProductionRecordResponse> getRecordsByStage(Long stageId) {
        return recordRepository.findByStageId(stageId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public ProductionRecordResponse getRecordById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public ProductionRecordResponse createRecord(ProductionRecordRequest request) {
        ProductionLine line = lineRepository.findById(request.getProductionLineId())
                .orElseThrow(() -> new ResourceNotFoundException("ProductionLine", request.getProductionLineId()));
        ProductionStage stage = stageRepository.findById(request.getStageId())
                .orElseThrow(() -> new ResourceNotFoundException("ProductionStage", request.getStageId()));

        ProductionRecord record = new ProductionRecord();
        record.setProductionLine(line);
        record.setStage(stage);
        record.setTimestamp(request.getTimestamp() != null ? request.getTimestamp() : LocalDateTime.now());
        record.setShift(request.getShift());
        record.setUnitsProduced(request.getUnitsProduced());
        record.setDefectiveUnits(request.getDefectiveUnits());
        record.setProcessingTimeMinutes(request.getProcessingTimeMinutes());
        record.setWaitingTimeMinutes(request.getWaitingTimeMinutes());
        record.setDowntimeMinutes(request.getDowntimeMinutes());
        record.setTotalWindowMinutes(request.getTotalWindowMinutes() > 0 ? request.getTotalWindowMinutes() : 480.0);

        if (request.getMachineId() != null) {
            Machine machine = machineRepository.findById(request.getMachineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Machine", request.getMachineId()));
            record.setMachine(machine);
        }

        return toResponse(recordRepository.save(record));
    }

    @Transactional
    public void deleteRecord(Long id) {
        recordRepository.delete(findOrThrow(id));
    }

    private ProductionRecord findOrThrow(Long id) {
        return recordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductionRecord", id));
    }

    public ProductionRecordResponse toResponse(ProductionRecord r) {
        ProductionRecordResponse resp = new ProductionRecordResponse();
        resp.setId(r.getId());
        resp.setProductionLineId(r.getProductionLine().getId());
        resp.setProductionLineName(r.getProductionLine().getName());
        resp.setStageId(r.getStage().getId());
        resp.setStageName(r.getStage().getName());
        if (r.getMachine() != null) {
            resp.setMachineId(r.getMachine().getId());
            resp.setMachineName(r.getMachine().getName());
        }
        resp.setTimestamp(r.getTimestamp());
        resp.setShift(r.getShift());
        resp.setUnitsProduced(r.getUnitsProduced());
        resp.setDefectiveUnits(r.getDefectiveUnits());
        resp.setProcessingTimeMinutes(r.getProcessingTimeMinutes());
        resp.setWaitingTimeMinutes(r.getWaitingTimeMinutes());
        resp.setDowntimeMinutes(r.getDowntimeMinutes());
        resp.setTotalWindowMinutes(r.getTotalWindowMinutes());
        return resp;
    }
}
