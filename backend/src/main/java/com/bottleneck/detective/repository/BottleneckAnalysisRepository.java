package com.bottleneck.detective.repository;

import com.bottleneck.detective.entity.BottleneckAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BottleneckAnalysisRepository extends JpaRepository<BottleneckAnalysis, Long> {

    /** All analyses for a given line, newest first */
    List<BottleneckAnalysis> findByProductionLineIdOrderByAnalyzedAtDesc(Long productionLineId);

    /** The most recent analysis for a line */
    Optional<BottleneckAnalysis> findFirstByProductionLineIdOrderByAnalyzedAtDesc(Long productionLineId);
}
