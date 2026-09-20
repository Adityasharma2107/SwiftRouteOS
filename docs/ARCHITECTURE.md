# SwiftRouteOS: Deep Dive Architecture & System Specifications

## 1. Domain Data Model & Entity-Relationship Architecture

SwiftRouteOS persists its operational state across a normalized PostgreSQL 16 schema managed through sequential Flyway migrations (`V1__init_schema.sql` and `V2__seed_reference_data.sql`).

```mermaid
erDiagram
    USERS ||--o{ JOBS : creates
    USERS ||--o{ AUDIT_EVENTS : triggers
    TECHNICIANS ||--o{ TECHNICIAN_SKILLS : possesses
    SKILLS ||--o{ TECHNICIAN_SKILLS : categorized_in
    SKILLS ||--o{ JOB_REQUIRED_SKILLS : required_by
    JOBS ||--o{ JOB_REQUIRED_SKILLS : requires
    JOBS ||--o{ JOB_PARTS : needs
    JOBS ||--o{ INVENTORY_RESERVATIONS : holds
    JOBS ||--o{ SLA_ESCALATIONS : experiences
    JOBS ||--o{ AUDIT_EVENTS : tracks
    TECHNICIANS ||--o{ JOBS : assigned_to
    INVENTORY_ITEMS ||--o{ JOB_PARTS : references
    INVENTORY_ITEMS ||--o{ INVENTORY_RESERVATIONS : locked_for
    SLA_POLICIES ||--o{ JOBS : governs

    USERS {
        bigint id PK
        varchar username UK
        varchar password_hash
        varchar full_name
        varchar email
        varchar role "ADMIN | DISPATCHER | TECHNICIAN"
        boolean enabled
        timestamptz created_at
    }

    TECHNICIANS {
        bigint id PK
        varchar name
        varchar email
        varchar phone
        varchar status "AVAILABLE | BUSY | ON_BREAK | OFFLINE"
        numeric current_latitude
        numeric current_longitude
        time shift_start
        time shift_end
        integer max_daily_jobs
        integer current_job_count
        timestamptz created_at
    }

    JOBS {
        bigint id PK
        varchar title
        text description
        varchar priority "CRITICAL | HIGH | MEDIUM | LOW"
        varchar status "PENDING | ASSIGNED | EN_ROUTE | IN_PROGRESS | COMPLETED | CANCELLED"
        numeric customer_latitude
        numeric customer_longitude
        varchar customer_address
        varchar customer_name
        varchar customer_phone
        bigint assigned_technician_id FK
        bigint created_by_id FK
        timestamptz scheduled_start_at
        timestamptz scheduled_end_at
        timestamptz target_response_at
        timestamptz target_resolution_at
        timestamptz actual_response_at
        timestamptz actual_resolution_at
        timestamptz created_at
    }

    INVENTORY_ITEMS {
        bigint id PK
        varchar sku UK
        varchar name
        varchar category
        integer quantity_on_hand
        integer quantity_allocated
        integer reorder_point
        numeric unit_cost
        timestamptz updated_at
    }

    INVENTORY_RESERVATIONS {
        bigint id PK
        bigint job_id FK
        bigint item_id FK
        integer quantity_reserved
        varchar status "RESERVED | CONSUMED | RELEASED"
        timestamptz reserved_at
        timestamptz consumed_at
        timestamptz released_at
    }

    SLA_POLICIES {
        bigint id PK
        varchar priority UK "CRITICAL | HIGH | MEDIUM | LOW"
        integer target_response_minutes
        integer target_resolution_minutes
        boolean active
    }

    SLA_ESCALATIONS {
        bigint id PK
        bigint job_id FK
        varchar threshold_stage "NEARING_BREACH | BREACHED"
        timestamptz escalated_at
        text notification_payload
    }

    AUDIT_EVENTS {
        bigint id PK
        bigint job_id FK
        varchar event_type
        text description
        bigint triggered_by_id FK
        timestamptz created_at
    }
```

---

## 2. Pessimistic Locking & Deadlock Elimination Mechanics

### The Distributed Concurrency Problem
Field service organizations encounter critical race conditions when multiple dispatchers or technicians simultaneously claim scarce replacement parts (e.g., an industrial chiller compressor sensor with `quantity_on_hand = 1`). Optimistic concurrency (`@Version`) would abort transactions after lengthy business logic execution, wasting I/O and creating frustrating user retries.

### Database Row Lock Acquisition Sequence
SwiftRouteOS implements strict database-level row locking with defensive deadlock ordering:

```mermaid
sequenceDiagram
    autonumber
    actor TechA as Technician Dave (Thread 1)
    actor TechB as Technician Elena (Thread 2)
    participant Svc as InventoryService
    participant DB as PostgreSQL 16 (Row Lock Engine)

    TechA->>Svc: Reserve Part #42 (Qty: 1)
    TechB->>Svc: Reserve Part #42 (Qty: 1)
    Note over Svc,DB: Both requests enter @Transactional boundary

    rect rgb(230, 245, 230)
    Note over TechA,DB: Thread 1 acquires lock first
    Svc->>DB: SELECT * FROM inventory_items WHERE id = 42 FOR UPDATE;
    DB-->>Svc: Row locked by Thread 1 (quantity_on_hand=1, allocated=0)
    end

    rect rgb(255, 240, 240)
    Note over TechB,DB: Thread 2 contends for same row
    Svc->>DB: SELECT * FROM inventory_items WHERE id = 42 FOR UPDATE;
    DB-->>DB: Thread 2 BLOCKS at engine row-level waiting for lock release
    end

    Svc->>DB: Check available: (1 - 0) >= 1 (OK)
    Svc->>DB: UPDATE inventory_items SET quantity_allocated = 1 WHERE id = 42;
    Svc->>DB: INSERT INTO inventory_reservations (job_id, item_id, 1, 'RESERVED');
    Svc->>DB: COMMIT TRANSACTION;
    DB-->>TechA: 201 Created (Reservation Confirmed)

    rect rgb(255, 240, 240)
    Note over TechB,DB: Thread 2 unblocks upon COMMIT
    DB-->>Svc: Row acquired by Thread 2 (quantity_on_hand=1, allocated=1)
    Svc->>DB: Check available: (1 - 1) >= 1 (FALSE!)
    Svc->>DB: ROLLBACK TRANSACTION;
    Svc-->>TechB: 409 Conflict: "Insufficient stock for item #42"
    end
```

### Multi-Item Deadlock Prevention
When reserving multiple parts in a single transaction, locking items in arbitrary order creates circular wait dependencies:
$$\text{Thread 1: Locks Item A} \rightarrow \text{Waits for Item B}$$
$$\text{Thread 2: Locks Item B} \rightarrow \text{Waits for Item A} \implies \text{DEADLOCK!}$$

**Elimination Strategy**: SwiftRouteOS enforces **Natural Ascending ID Ordering** before executing `FOR UPDATE`:
```java
// InventoryService.java
List<Long> orderedItemIds = request.getItemReservations().stream()
    .map(ItemReservationRequest::getItemId)
    .sorted() // Deterministic ascending order
    .toList();

for (Long itemId : orderedItemIds) {
    InventoryItem item = inventoryItemRepository.findByIdForUpdate(itemId)
        .orElseThrow(...);
    // Safe sequential lock acquisition
}
```

---

## 3. SLA Escalation Engine State Machine

The SLA Escalation Engine evaluates open service requests against pre-configured response and resolution thresholds.

```mermaid
stateDiagram-v2
    [*] --> HEALTHY: Job Created (Target Deadlines Injected)
    
    HEALTHY --> NEARING_BREACH: Elapsed Time >= 70% of SLA Window
    note right of NEARING_BREACH
        - Record SLA_ESCALATIONS (stage='NEARING_BREACH')
        - Broadcast SLA_WARNING to /topic/sla-alerts
        - Display Amber Toast in UI
    end note

    NEARING_BREACH --> BREACHED: Elapsed Time >= 100% of SLA Window
    HEALTHY --> BREACHED: Immediate Breach (e.g. Backdated)
    note right of BREACHED
        - Record SLA_ESCALATIONS (stage='BREACHED')
        - Broadcast SLA_BREACHED to /topic/sla-alerts
        - Display Glowing Rose Modal/Toast in UI
    end note

    HEALTHY --> COMPLETED: Transitioned to COMPLETED before deadline
    NEARING_BREACH --> COMPLETED: Completed within SLA warning margin
    BREACHED --> COMPLETED: Completed after breach (Recorded for Penalty Analytics)

    COMPLETED --> [*]
```

### Idempotency Guarantee
The background worker executes every 30 seconds (`@Scheduled(fixedRateString = "${swiftroute.sla.scheduler-rate-ms:30000}")`). To prevent alert spamming:
1. The table `sla_escalations` enforces:
   ```sql
   CONSTRAINT uq_sla_job_threshold UNIQUE (job_id, threshold_stage)
   ```
2. When the worker detects a job at $\ge 70\%$ or $\ge 100\%$, it checks `slaEscalationRepository.existsByJobIdAndThresholdStage(jobId, stage)`.
3. If concurrent worker threads overlap, the database constraint rejects the duplicate insert, ensuring zero duplicate alerts.

---

## 4. Haversine Spatial Geometry & Multi-Factor Scoring

### Spherical Distance Formula
Given technician coordinates $(\phi_1, \lambda_1)$ and customer site coordinates $(\phi_2, \lambda_2)$ in radians:
$$\Delta\phi = \phi_2 - \phi_1, \quad \Delta\lambda = \lambda_2 - \lambda_1$$
$$a = \sin^2\left(\frac{\Delta\phi}{2}\right) + \cos(\phi_1)\cos(\phi_2)\sin^2\left(\frac{\Delta\lambda}{2}\right)$$
$$c = 2 \cdot \text{atan2}\left(\sqrt{a}, \sqrt{1 - a}\right)$$
$$\text{Distance} = R \cdot c \quad (R = 6,371.0 \text{ km})$$

Implemented in pure Java without third-party dependencies, guaranteeing $O(1)$ computation time and 100% deterministic unit testing.

### Candidate Ranking Formula
$$TotalScore = S_{skill} + S_{workload} + S_{shift} - P_{dist}$$

| Factor | Weight Range | Calculation Logic |
|---|---|---|
| **Skill Match** | $0 \dots 40$ pts | Ratio of matched required skills: $(N_{matched} / N_{required}) \cdot 40$ |
| **Workload** | $0 \dots 25$ pts | Inverse daily load: $\max(0, 25 - (current\_jobs \cdot 5))$ |
| **Shift Window** | $0 \dots 20$ pts | Full points if shift end is $> 2$ hours away; decrements linearly |
| **Distance Penalty** | $-0.5 \text{ pts} / \text{km}$ | Decrement proportional to spatial separation: $\min(30, \text{Distance}_{km} \cdot 0.5)$ |

---

## 5. Security & Authentication Architecture

SwiftRouteOS employs stateless, cryptographically secure JWT authentication:
* **Access Token**: Short-lived (default 24 hours), carrying user ID, username, and assigned authority (`ROLE_DISPATCHER`, `ROLE_TECHNICIAN`, `ROLE_ADMIN`).
* **Refresh Token**: Long-lived (default 7 days), securely stored in browser local storage and used for silent background refreshing on HTTP 401.
* **Axios Interceptor**: Queues failed requests while refreshing the token, replaying pending calls transparently without dropping user actions.
* **RBAC Enforcement**: Declarative method security (`@PreAuthorize("hasRole('ADMIN')")`) backed by a strict Spring Security filter chain.
