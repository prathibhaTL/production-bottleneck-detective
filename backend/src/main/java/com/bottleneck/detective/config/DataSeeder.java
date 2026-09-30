package com.bottleneck.detective.config;

import com.bottleneck.detective.entity.*;
import com.bottleneck.detective.repository.*;
import com.bottleneck.detective.service.FactorySimulatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * DataSeeder
 * ──────────
 * Runs ONCE when the application starts.
 * If no production lines exist yet, it creates a complete demo factory
 * with two production lines, machines, stages, and simulated records.
 *
 * CommandLineRunner is a Spring Boot interface – any bean that implements it
 * has its {@code run()} method called automatically after the application context loads.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ProductionLineRepository lineRepository;
    private final MachineRepository machineRepository;
    private final ProductionStageRepository stageRepository;
    private final FactorySimulatorService simulatorService;

    @Override
    public void run(String... args) {
        // Only seed if the database is empty
        if (lineRepository.count() > 0) {
            log.info("Demo data already present – skipping seeder.");
            return;
        }

        log.info("=== Seeding demo data ===");

        // ── Machines ───────────────────────────────────────────────────────
        Machine m1 = machine("CNC Lathe L-01",       "CNC Lathe",      "LT-2020");
        Machine m2 = machine("Welding Robot W-02",   "Robotic Welder", "WR-500X");
        Machine m3 = machine("Paint Booth PB-03",    "Paint Booth",    "PB-Classic");
        Machine m4 = machine("Assembly Robot AR-04", "Robotic Arm",    "UR-10");
        Machine m5 = machine("Quality Scanner QC-05","Vision Scanner", "VS-Pro");
        Machine m6 = machine("Conveyor CV-06",       "Conveyor Belt",  "CV-Heavy");
        Machine m7 = machine("Cutting Machine CM-07","Laser Cutter",   "LC-2023");
        Machine m8 = machine("Polisher PL-08",       "Surface Polisher","SP-400");

        // ── Production Line 1 – Automotive Parts ──────────────────────────
        ProductionLine line1 = new ProductionLine();
        line1.setName("Line A – Automotive Parts");
        line1.setDescription("Produces engine mounts and chassis components");
        line1.setStatus("Active");
        lineRepository.save(line1);

        stage(line1, "Raw Material Prep",   1, 80,  2.0, m6, "Active");
        stage(line1, "CNC Machining",       2, 50,  4.5, m1, "Active");
        stage(line1, "Welding",             3, 40,  6.0, m2, "Active"); // ← problem stage
        stage(line1, "Surface Treatment",   4, 60,  3.5, m8, "Active");
        stage(line1, "Quality Inspection",  5, 70,  2.5, m5, "Active");

        log.info("Created Line 1 with 5 stages");

        // ── Production Line 2 – Electronics Assembly ──────────────────────
        ProductionLine line2 = new ProductionLine();
        line2.setName("Line B – Electronics Assembly");
        line2.setDescription("Assembles PCBs and final electronic units");
        line2.setStatus("Active");
        lineRepository.save(line2);

        stage(line2, "Component Cutting",   1, 100, 1.5, m7, "Active");
        stage(line2, "Soldering",           2, 60,  3.0, m2, "Active");
        stage(line2, "Painting & Coating",  3, 45,  5.5, m3, "Active"); // ← problem stage
        stage(line2, "Final Assembly",      4, 55,  4.0, m4, "Active");
        stage(line2, "Testing & QC",        5, 65,  3.0, m5, "Active");

        log.info("Created Line 2 with 5 stages");

        // ── Generate simulated production history ─────────────────────────
        log.info("Generating simulated records for Line 1...");
        simulatorService.runSimulation(line1.getId(), 48);

        log.info("Generating simulated records for Line 2...");
        simulatorService.runSimulation(line2.getId(), 48);

        log.info("=== Demo data seeding complete ===");
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Machine machine(String name, String type, String model) {
        Machine m = new Machine();
        m.setName(name);
        m.setType(type);
        m.setModelNumber(model);
        m.setStatus("Running");
        return machineRepository.save(m);
    }

    private ProductionStage stage(ProductionLine line, String name, int order,
                                   double capacityPerHour, double stdTime,
                                   Machine machine, String status) {
        ProductionStage s = new ProductionStage();
        s.setName(name);
        s.setStageOrder(order);
        s.setCapacityPerHour(capacityPerHour);
        s.setStandardProcessingTimeMinutes(stdTime);
        s.setMachine(machine);
        s.setStatus(status);
        s.setProductionLine(line);
        return stageRepository.save(s);
    }
}
