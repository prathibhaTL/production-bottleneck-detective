package com.bottleneck.detective.repository;

import com.bottleneck.detective.entity.ProductionLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data automatically implements all CRUD methods for ProductionLine.
 * You never write SQL for basic operations – Spring generates it.
 */
@Repository
public interface ProductionLineRepository extends JpaRepository<ProductionLine, Long> {

    /** Find all lines by status (e.g., "Active", "Inactive") */
    List<ProductionLine> findByStatus(String status);

    /** Check if a line with this exact name already exists */
    boolean existsByName(String name);
}
