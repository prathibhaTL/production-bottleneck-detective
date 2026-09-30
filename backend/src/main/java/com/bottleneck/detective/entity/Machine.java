package com.bottleneck.detective.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents a physical machine on the factory floor.
 * Machines are assigned to production stages and generate production records.
 */
@Entity
@Table(name = "machines")
@Getter
@Setter
@NoArgsConstructor
public class Machine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Machine name is required")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    /** E.g., "CNC Lathe", "Conveyor", "Robotic Arm" */
    @Size(max = 100)
    private String type;

    /** Model or serial identifier */
    @Size(max = 100)
    private String modelNumber;

    /** Running / Down / Maintenance / Idle */
    @Column(nullable = false, length = 20)
    private String status = "Running";

    /** When this machine was added to the system */
    @Column(nullable = false, updatable = false)
    private LocalDateTime installedAt = LocalDateTime.now();
}
