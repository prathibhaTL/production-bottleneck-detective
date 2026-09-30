# Production Bottleneck Detective

A full-stack manufacturing analytics application that identifies production bottlenecks, analyses possible root causes, and estimates the impact of improvements.

---

## Table of Contents
1. [Architecture](#architecture)
2. [Technology Stack](#technology-stack)
3. [Project Structure](#project-structure)
4. [Setup & Running](#setup--running)
5. [Bottleneck Detection Algorithm](#bottleneck-detection-algorithm)
6. [What-If Simulator Logic](#what-if-simulator-logic)
7. [Factory Simulator](#factory-simulator)
8. [REST API Reference](#rest-api-reference)
9. [Running Tests](#running-tests)
10. [Understanding the Code (Beginner Guide)](#understanding-the-code-beginner-guide)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        Browser                              │
│               React + Vite (port 5173)                      │
└────────────────────────┬────────────────────────────────────┘
                         │  HTTP REST (via Vite proxy → 8080)
┌────────────────────────▼────────────────────────────────────┐
│          Spring Boot Backend (port 8080)                    │
│  Controllers → Services → Repositories → JPA Entities       │
└────────────────────────┬────────────────────────────────────┘
                         │  JDBC
┌────────────────────────▼────────────────────────────────────┐
│                    MySQL Database                           │
│              bottleneck_db                                  │
└─────────────────────────────────────────────────────────────┘
```

**Request flow (example: run bottleneck analysis)**

1. User clicks "Run Analysis" in React.
2. `api.js` sends `GET /api/analysis/bottleneck/1`.
3. Vite proxy forwards the request to Spring Boot on port 8080.
4. `BottleneckAnalysisController` receives the request.
5. It calls `BottleneckAnalysisService.analyzeBottleneck()`.
6. Service fetches `ProductionRecord` rows from MySQL via JPA repositories.
7. Calculates metrics, scores each stage, identifies the bottleneck.
8. Saves result to `bottleneck_analyses` table.
9. Returns `BottleneckResult` DTO as JSON.
10. React renders the result in the Analysis page.

---

## Technology Stack

| Layer      | Technology                       | Why                                    |
|------------|----------------------------------|----------------------------------------|
| Backend    | Java 17, Spring Boot 3.2         | Industry-standard, beginner-friendly   |
| ORM        | Spring Data JPA + Hibernate      | Avoids raw SQL for basic CRUD          |
| Database   | MySQL 8                          | Reliable relational database           |
| Validation | Bean Validation (Hibernate)      | Declarative input validation           |
| Frontend   | React 18, Vite                   | Fast development, clean component model|
| Charts     | Recharts                         | Simple React chart library             |
| HTTP       | Axios / Fetch API                | REST client in the browser             |
| Tests      | JUnit 5, AssertJ                 | Readable unit tests                    |
| Build      | Maven (backend), npm (frontend)  |                                        |

---

## Project Structure

```
Production Bottleneck Detective/
├── backend/
│   ├── pom.xml                         ← Maven build file
│   ├── .env.example                    ← Environment variable reference
│   └── src/
│       ├── main/java/com/bottleneck/detective/
│       │   ├── ProductionBottleneckDetectiveApplication.java  ← Entry point
│       │   ├── config/
│       │   │   └── DataSeeder.java         ← Auto-loads demo data on startup
│       │   ├── controller/                 ← REST endpoints
│       │   │   ├── ProductionLineController.java
│       │   │   ├── MachineController.java
│       │   │   ├── ProductionStageController.java
│       │   │   ├── ProductionRecordController.java
│       │   │   ├── BottleneckAnalysisController.java
│       │   │   ├── WhatIfSimulatorController.java
│       │   │   ├── FactorySimulatorController.java
│       │   │   └── DashboardController.java
│       │   ├── dto/                        ← Data Transfer Objects (API shapes)
│       │   ├── entity/                     ← JPA database entities
│       │   │   ├── ProductionLine.java
│       │   │   ├── ProductionStage.java
│       │   │   ├── Machine.java
│       │   │   ├── ProductionRecord.java
│       │   │   ├── DowntimeEvent.java
│       │   │   └── BottleneckAnalysis.java
│       │   ├── exception/                  ← Error handling
│       │   ├── repository/                 ← JPA data access interfaces
│       │   └── service/                    ← Business logic
│       │       ├── BottleneckAnalysisService.java  ← Core algorithm
│       │       ├── WhatIfSimulatorService.java
│       │       ├── FactorySimulatorService.java
│       │       └── DashboardService.java
│       ├── main/resources/
│       │   ├── application.properties        ← Active config (NOT committed)
│       │   └── application.properties.example
│       └── test/java/com/bottleneck/detective/
│           └── service/
│               └── BottleneckAnalysisServiceTest.java
└── frontend/
    ├── package.json
    ├── vite.config.js
    ├── index.html
    └── src/
        ├── main.jsx        ← React entry point
        ├── App.jsx         ← Router + sidebar layout
        ├── App.css         ← Global styles
        ├── api.js          ← All HTTP calls to backend
        └── pages/
            ├── Dashboard.jsx   ← KPI tiles, charts, pipeline view
            ├── Analysis.jsx    ← Bottleneck analysis + radar chart
            ├── WhatIf.jsx      ← What-if slider simulator
            ├── Simulator.jsx   ← Factory event simulator
            ├── Lines.jsx       ← Manage production lines & stages
            └── Machines.jsx    ← Manage machines
```

---

## Setup & Running

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8 running locally
- Node.js 18+ and npm

### 1. Database
MySQL will be created automatically by Hibernate (`createDatabaseIfNotExist=true`).
No manual SQL required.

### 2. Backend

```bash
cd backend

# Copy and edit the config file
cp src/main/resources/application.properties.example \
   src/main/resources/application.properties

# Edit application.properties:
#   spring.datasource.username=YOUR_USERNAME
#   spring.datasource.password=YOUR_PASSWORD

# Start the server
mvn spring-boot:run
```

The server starts on **http://localhost:8080**.
Demo data (2 production lines, machines, stages, 48h of simulated records) is loaded automatically on first start.

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

The app opens on **http://localhost:5173**.

---

## Bottleneck Detection Algorithm

The algorithm is implemented in [`BottleneckAnalysisService`](backend/src/main/java/com/bottleneck/detective/service/BottleneckAnalysisService.java).

### Step-by-step

1. **Collect records** — fetch all `ProductionRecord` rows for the line within the requested time window.
2. **Group by stage** — build a map of `stageId → List<ProductionRecord>`.
3. **Compute 6 metrics per stage:**

   | Metric | Formula | Meaning |
   |--------|---------|---------|
   | Capacity Utilization | `(unitsProduced / (capacityPerHour × windowHours)) × 100` | How loaded is the stage? |
   | Throughput | `unitsProduced / windowHours` | Output rate |
   | Avg Processing Time | Mean of `processingTimeMinutes` | How long does one unit take? |
   | Avg Waiting Time | Mean of `waitingTimeMinutes` | Queue buildup indicator |
   | Downtime % | `(totalDowntime / totalWindow) × 100` | Machine availability |
   | Defect Rate | `(defectiveUnits / unitsProduced) × 100` | Quality loss |

4. **Score each stage** (0–100, weighted sum):

   | Metric | Weight | Rationale |
   |--------|--------|-----------|
   | Capacity utilization | 20% | High usage → overloaded |
   | Downtime % | 25% | Downtime directly destroys capacity |
   | Avg waiting time | 25% | Queue buildup = blocked upstream stages |
   | Avg processing time | 15% | Slow cycle = line bottleneck |
   | Defect rate | 15% | Defects consume capacity through rework |

5. **Identify bottleneck** — the stage with the highest score.
6. **Flag possible contributing factors** using threshold checks:
   - Utilization ≥ 85%
   - Downtime ≥ 15%
   - Avg wait ≥ 10 min
   - Avg processing ≥ 8 min/unit
   - Defect rate ≥ 5%
7. **Determine severity:**
   - 0–30: LOW
   - 30–55: MEDIUM
   - 55–75: HIGH
   - 75+: CRITICAL
8. **Persist** the result to `bottleneck_analyses`.

> **Important:** Possible contributing factors are identified from correlation in the data,
> not from confirmed causal analysis. The language used ("may indicate", "possibly") reflects this.

---

## What-If Simulator Logic

Implemented in [`WhatIfSimulatorService`](backend/src/main/java/com/bottleneck/detective/service/WhatIfSimulatorService.java).

Takes the current bottleneck's metrics as a baseline and applies four linear estimations:

| Parameter | Model |
|-----------|-------|
| Downtime reduction | Recovered time → extra units: `recoveredFraction × (60/processingMin)` units/hr |
| Processing time reduction | Faster cycle → proportional throughput gain: `throughput / (1 - reductionFactor)` |
| Additional capacity | Direct addition to throughput |
| Defect rate reduction | Yield improvement: `throughput × (newYield / oldYield)` |

All results carry a disclaimer that they are **estimates** based on simplified linear assumptions.

---

## Factory Simulator

Implemented in [`FactorySimulatorService`](backend/src/main/java/com/bottleneck/detective/service/FactorySimulatorService.java).

- Generates `periods × stages` production records, one per hour per stage.
- **One stage is randomly designated as the "problem stage"** per simulation run.
- The problem stage receives: elevated downtime (10–35 min/hr), extra waiting time (8–20 min), extra processing time (2–8 min/unit), and 1.5–2.5× defect rate.
- Normal stages occasionally get minor random events (15% probability).
- All downtime events > 5 minutes are also saved to `downtime_events`.

---

## REST API Reference

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/production-lines` | List all lines |
| POST | `/api/production-lines` | Create a line |
| PUT | `/api/production-lines/{id}` | Update a line |
| DELETE | `/api/production-lines/{id}` | Delete a line |
| GET | `/api/stages?lineId={id}` | List stages for a line |
| POST | `/api/stages` | Create a stage |
| DELETE | `/api/stages/{id}` | Delete a stage |
| GET | `/api/machines` | List all machines |
| POST | `/api/machines` | Create a machine |
| GET | `/api/records?lineId={id}` | Records for a line |
| POST | `/api/records` | Add a production record |
| GET | `/api/analysis/bottleneck/{lineId}?hoursBack=48` | Run & return analysis |
| POST | `/api/simulation/what-if/{lineId}` | Run what-if simulation |
| POST | `/api/simulator/run/{lineId}?periods=24` | Run factory simulator |
| GET | `/api/dashboard/{lineId}` | Dashboard summary |

---

## Running Tests

```bash
cd backend
mvn test
```

Tests are in [`BottleneckAnalysisServiceTest`](backend/src/test/java/com/bottleneck/detective/service/BottleneckAnalysisServiceTest.java).
They run **without a database** — the algorithm is tested in pure Java with no Spring context.

---

## Understanding the Code (Beginner Guide)

### Why DTOs?
An entity is a mirror of the database table. A DTO (Data Transfer Object) is what the API
sends/receives. They are separate so that:
- You can rename database columns without breaking the API.
- You can control exactly which fields are exposed.
- You can add computed fields (e.g., `stageCount`) without storing them.

### Why `@Service`, `@Repository`, `@Controller`?
These are Spring **stereotypes** — annotations that tell Spring to create and manage instances
of these classes. You never write `new ProductionLineService()` — Spring injects it for you.

### Why `@Transactional`?
It wraps the method in a database transaction. If anything fails mid-method, all changes
are rolled back. Without it, you could end up with partially-saved data.

### Why `@RequiredArgsConstructor`?
Lombok generates a constructor that sets all `final` fields. Spring uses this constructor
to inject dependencies (services, repositories). This is called **constructor injection**
and is the recommended pattern.

### The Repository pattern
`JpaRepository<ProductionLine, Long>` gives you `save()`, `findById()`, `findAll()`,
`delete()` etc. for free. You only need to add methods for custom queries.
Spring Data generates the SQL from the method name, e.g.:
`findByProductionLineIdOrderByStageOrder` → `SELECT ... WHERE production_line_id = ? ORDER BY stage_order`.
