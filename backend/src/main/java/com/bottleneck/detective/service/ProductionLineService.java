package com.bottleneck.detective.service;

import com.bottleneck.detective.dto.ProductionLineRequest;
import com.bottleneck.detective.dto.ProductionLineResponse;
import com.bottleneck.detective.entity.ProductionLine;
import com.bottleneck.detective.exception.BadRequestException;
import com.bottleneck.detective.exception.ResourceNotFoundException;
import com.bottleneck.detective.repository.ProductionLineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for managing production lines.
 *
 * @Service marks this as a Spring-managed service bean.
 * @RequiredArgsConstructor generates a constructor that injects all final fields.
 * @Transactional ensures database operations are wrapped in a transaction.
 */
@Service
@RequiredArgsConstructor
public class ProductionLineService {

    private final ProductionLineRepository productionLineRepository;

    public List<ProductionLineResponse> getAllLines() {
        return productionLineRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProductionLineResponse getLineById(Long id) {
        ProductionLine line = findOrThrow(id);
        return toResponse(line);
    }

    @Transactional
    public ProductionLineResponse createLine(ProductionLineRequest request) {
        if (productionLineRepository.existsByName(request.getName())) {
            throw new BadRequestException("A production line named '" + request.getName() + "' already exists.");
        }

        ProductionLine line = new ProductionLine();
        line.setName(request.getName());
        line.setDescription(request.getDescription());
        line.setStatus(request.getStatus() != null ? request.getStatus() : "Active");

        return toResponse(productionLineRepository.save(line));
    }

    @Transactional
    public ProductionLineResponse updateLine(Long id, ProductionLineRequest request) {
        ProductionLine line = findOrThrow(id);

        // Only block duplicate name if the name actually changed
        if (!line.getName().equals(request.getName())
                && productionLineRepository.existsByName(request.getName())) {
            throw new BadRequestException("A production line named '" + request.getName() + "' already exists.");
        }

        line.setName(request.getName());
        line.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            line.setStatus(request.getStatus());
        }

        return toResponse(productionLineRepository.save(line));
    }

    @Transactional
    public void deleteLine(Long id) {
        ProductionLine line = findOrThrow(id);
        productionLineRepository.delete(line);
    }

    // ---- Private helpers ----

    private ProductionLine findOrThrow(Long id) {
        return productionLineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductionLine", id));
    }

    /**
     * Converts a JPA entity to a DTO (Response object).
     * We never send entity objects directly to the client – this decouples
     * the API contract from the database schema.
     */
    public ProductionLineResponse toResponse(ProductionLine line) {
        ProductionLineResponse resp = new ProductionLineResponse();
        resp.setId(line.getId());
        resp.setName(line.getName());
        resp.setDescription(line.getDescription());
        resp.setStatus(line.getStatus());
        resp.setCreatedAt(line.getCreatedAt());
        resp.setStageCount(line.getStages() != null ? line.getStages().size() : 0);
        return resp;
    }
}
