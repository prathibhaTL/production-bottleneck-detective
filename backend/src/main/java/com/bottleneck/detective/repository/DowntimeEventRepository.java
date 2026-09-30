package com.bottleneck.detective.repository;

import com.bottleneck.detective.entity.DowntimeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DowntimeEventRepository extends JpaRepository<DowntimeEvent, Long> {

    List<DowntimeEvent> findByMachineId(Long machineId);

    List<DowntimeEvent> findByStageId(Long stageId);

    /** Active downtime events (machine is still down – no endTime) */
    List<DowntimeEvent> findByMachineIdAndEndTimeIsNull(Long machineId);
}
