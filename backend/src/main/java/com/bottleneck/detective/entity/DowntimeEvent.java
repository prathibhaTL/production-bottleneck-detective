package com.bottleneck.detective.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Records a specific downtime event for a machine.
 *
 * Downtime events are separate from the aggregate downtimeMinutes stored
 * in ProductionRecord.  They allow you to audit individual incidents –
 * e.g., "Machine M-3 broke down on Tuesday at 14:05 for 45 minutes".
 */
@Entity
@Table(name = "downtime_events")
@Getter
@Setter
@NoArgsConstructor
public class DowntimeEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id")
    private ProductionStage stage;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime startTime;

    /** Null if the machine has not come back online yet */
    private LocalDateTime endTime;

    /**
     * Duration in minutes – filled in automatically when endTime is set,
     * or stored directly when importing historical data.
     */
    private Double durationMinutes;

    /**
     * Why the machine went down.
     * E.g., "Mechanical failure", "Scheduled maintenance", "Power outage".
     * Labelled as possible cause – may not always be confirmed.
     */
    @NotBlank
    @Column(nullable = false, length = 200)
    private String reason;

    /**
     * Severity of the event: LOW / MEDIUM / HIGH / CRITICAL
     */
    @Column(length = 20)
    private String severity = "MEDIUM";
}
