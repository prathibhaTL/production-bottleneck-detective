package com.bottleneck.detective.repository;

import com.bottleneck.detective.entity.ProductionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductionRecordRepository extends JpaRepository<ProductionRecord, Long> {

    /** All records for a specific stage */
    List<ProductionRecord> findByStageId(Long stageId);

    /** All records for a specific production line */
    List<ProductionRecord> findByProductionLineId(Long productionLineId);

    /** Records for a line within a time window – used by the analysis engine */
    List<ProductionRecord> findByProductionLineIdAndTimestampBetween(
            Long productionLineId, LocalDateTime from, LocalDateTime to);

    /** Records for a stage within a time window */
    List<ProductionRecord> findByStageIdAndTimestampBetween(
            Long stageId, LocalDateTime from, LocalDateTime to);

    /** Latest N records for a line (for the dashboard trend chart) */
    List<ProductionRecord> findTop50ByProductionLineIdOrderByTimestampDesc(Long productionLineId);

    /**
     * Aggregate total units produced per stage for a production line.
     * Returns an Object[] of [stageId, stageName, totalUnits].
     */
    @Query("SELECT r.stage.id, r.stage.name, SUM(r.unitsProduced) " +
           "FROM ProductionRecord r " +
           "WHERE r.productionLine.id = :lineId " +
           "GROUP BY r.stage.id, r.stage.name")
    List<Object[]> sumUnitsByStage(@Param("lineId") Long lineId);
}
