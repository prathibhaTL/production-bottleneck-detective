package com.bottleneck.detective.service;

import com.bottleneck.detective.dto.MachineRequest;
import com.bottleneck.detective.dto.MachineResponse;
import com.bottleneck.detective.entity.Machine;
import com.bottleneck.detective.exception.BadRequestException;
import com.bottleneck.detective.exception.ResourceNotFoundException;
import com.bottleneck.detective.repository.MachineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MachineService {

    private final MachineRepository machineRepository;

    public List<MachineResponse> getAllMachines() {
        return machineRepository.findAll()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public MachineResponse getMachineById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public MachineResponse createMachine(MachineRequest request) {
        if (machineRepository.existsByName(request.getName())) {
            throw new BadRequestException("A machine named '" + request.getName() + "' already exists.");
        }
        Machine machine = new Machine();
        machine.setName(request.getName());
        machine.setType(request.getType());
        machine.setModelNumber(request.getModelNumber());
        machine.setStatus(request.getStatus() != null ? request.getStatus() : "Running");
        return toResponse(machineRepository.save(machine));
    }

    @Transactional
    public MachineResponse updateMachine(Long id, MachineRequest request) {
        Machine machine = findOrThrow(id);
        if (!machine.getName().equals(request.getName())
                && machineRepository.existsByName(request.getName())) {
            throw new BadRequestException("A machine named '" + request.getName() + "' already exists.");
        }
        machine.setName(request.getName());
        machine.setType(request.getType());
        machine.setModelNumber(request.getModelNumber());
        if (request.getStatus() != null) machine.setStatus(request.getStatus());
        return toResponse(machineRepository.save(machine));
    }

    @Transactional
    public void deleteMachine(Long id) {
        machineRepository.delete(findOrThrow(id));
    }

    private Machine findOrThrow(Long id) {
        return machineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Machine", id));
    }

    public MachineResponse toResponse(Machine m) {
        MachineResponse r = new MachineResponse();
        r.setId(m.getId());
        r.setName(m.getName());
        r.setType(m.getType());
        r.setModelNumber(m.getModelNumber());
        r.setStatus(m.getStatus());
        r.setInstalledAt(m.getInstalledAt());
        return r;
    }

    public Machine findEntityById(Long id) {
        return findOrThrow(id);
    }
}
