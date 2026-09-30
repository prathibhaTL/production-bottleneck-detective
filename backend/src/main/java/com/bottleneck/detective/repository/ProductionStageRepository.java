package com.bottleneck.detective.repository;

import com.bottleneck.detective.entity.ProductionStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductionStageRepository extends JpaRepository<ProductionStage, Long> {

    /** All stages belonging to a given production line, ordered by their position */
    List<ProductionStage> findByProductionLineIdOrderByStageOrder(Long productionLineId);

    /** Stages filtered by status */
    List<ProductionStage> findByProductionLineIdAndStatus(Long productionLineId, String status);
}
