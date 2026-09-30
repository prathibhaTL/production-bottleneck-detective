package com.bottleneck.detective.repository;

import com.bottleneck.detective.entity.Machine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MachineRepository extends JpaRepository<Machine, Long> {

    List<Machine> findByStatus(String status);

    boolean existsByName(String name);
}
