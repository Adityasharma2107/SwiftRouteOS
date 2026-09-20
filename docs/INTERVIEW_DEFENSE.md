# SwiftRouteOS: Senior / Staff Engineer Interview Defense Guide

This guide is engineered to prepare developers and architects to defend every system design, concurrency, persistence, and architectural decision made in SwiftRouteOS during technical interviews.

---

## 🏛 Section 1: Concurrency & Transaction Architecture

### Q1: Why did you choose Pessimistic Locking (`SELECT ... FOR UPDATE`) over Optimistic Locking (`@Version`) for inventory reservation?
**Authoritative Answer:**
> "In high-velocity field service dispatch, physical inventory for scarce, high-value replacement parts (like industrial chiller transducers) is strictly limited—often with a quantity on hand of 1 or 2 units.
>
> If we used Optimistic Locking with `@Version`, ten concurrent dispatchers or automated batch jobs attempting to reserve the same last unit would all proceed past the read phase. Nine of them would fail at commit time with `OptimisticLockingFailureException`. Under high contention, optimistic locking causes:
> 1. **Mass retry storms** that degrade database CPU and connection pool availability.
> 2. **Poor operator UX**, where a dispatcher believes a part was available only to receive an error seconds later.
>
> By utilizing PostgreSQL database-level Pessimistic Row Locking (`SELECT ... FOR UPDATE`), the first transaction exclusively locks the specific inventory row. Subsequent transactions queue at the engine level. Once the first transaction commits and increments `quantity_allocated`, the queued transactions immediately see the updated stock, evaluate `(quantity_on_hand - quantity_allocated) < requested`, and return a deterministic `409 Conflict` without wasted computational churn."

---

### Q2: How did you prevent database deadlocks when reserving multiple inventory parts in a single transaction?
**Authoritative Answer:**
> "Deadlocks occur under multi-resource concurrency when transactions request locks in conflicting sequences. For example, Transaction 1 locks Item A and waits for Item B, while Transaction 2 locks Item B and waits for Item A (the Coffman 'Circular Wait' condition).
>
> We eliminated circular waits mathematically by enforcing a strict **Natural Ascending ID Ordering** across all multi-part reservation requests before locking:
> ```java
> List<Long> orderedItemIds = request.getItemReservations().stream()
>     .map(ItemReservationRequest::getItemId)
>     .sorted() // Guarantees global lock order
>     .toList();
> ```
> Because every transaction acquires row locks in identical numerical order ($Item_1 < Item_2 < \dots < Item_N$), a circular wait graph cannot materialize. We validated this under high concurrency with our multi-threaded test suite (`InventoryDeadlockPreventionTest.java`), which executes 10 concurrent threads reserving overlapping parts in inverted sequences without a single `DeadlockLoserDataAccessException`."

---

### Q3: What database transaction isolation level is used, and why?
**Authoritative Answer:**
> "We operate on PostgreSQL's default `READ COMMITTED` isolation level in conjunction with explicit row-level locks (`SELECT ... FOR UPDATE`).
>
> Using `SERIALIZABLE` globally would introduce excessive serialization anomalies (`40001` serialization failures) and require application-level retry wrappers around nearly every write operation. `READ COMMITTED` combined with pessimistic locking gives us exact row-level serialization on contended rows while allowing uninhibited parallel throughput on unrelated jobs, technicians, and catalog items."

---

## ⚡ Section 2: SLA Escalation Engine & Background Processing

### Q4: How do you prevent duplicate SLA escalation alerts and worker race conditions?
**Authoritative Answer:**
> "Background workers scheduled via `@Scheduled` or clustered background daemons can overlap if an evaluation cycle exceeds the fixed polling rate (30 seconds) or when multiple service instances run concurrently.
>
> We enforce idempotency at the database constraint layer rather than relying solely on in-memory application flags:
> ```sql
> CONSTRAINT uq_sla_job_threshold UNIQUE (job_id, threshold_stage);
> ```
> When the worker detects that a job has passed the 70% (`NEARING_BREACH`) or 100% (`BREACHED`) threshold:
> 1. It performs an indexed read check: `existsByJobIdAndThresholdStage(jobId, stage)`.
> 2. If absent, it attempts persistence inside an atomic transaction.
> 3. Even if two cluster nodes simultaneously evaluate the same breached job, PostgreSQL's unique constraint guarantees that only one row is committed, while the duplicate insert triggers a handled unique violation and aborts the secondary broadcast. This completely prevents alert spamming."

---

### Q5: How does the SLA sweep query scale as the `jobs` table grows to millions of rows?
**Authoritative Answer:**
> "A naive sweep running `SELECT * FROM jobs` and filtering in Java would cause massive memory overhead and database I/O bottlenecks.
>
> In SwiftRouteOS, the evaluation query is pushed entirely to the database with a high-selectivity composite index:
> ```sql
> CREATE INDEX idx_jobs_sla_eval 
> ON jobs (status, target_response_at, target_resolution_at) 
> WHERE status NOT IN ('COMPLETED', 'CANCELLED');
> ```
> By applying a **partial index** that excludes terminal states (`COMPLETED`, `CANCELLED`), the index only indexes the small working set of active tickets ($< 5\%$ of the total table volume), maintaining $O(\log N)$ B-tree index seeks regardless of historical archive size."

---

## 🗺 Section 3: Spatial Mathematics & Dispatch Optimization

### Q6: Why did you implement Haversine spherical geometry in-memory instead of using Google Maps Distance Matrix API?
**Authoritative Answer:**
> "For initial dispatch ranking, querying an external commercial routing API presents three architectural issues:
> 1. **Latency**: Calling an external HTTP API for 50 candidate technicians introduces 200–500ms of network overhead per dispatch calculation.
> 2. **Cost**: At thousands of dispatches per day with frequent re-ranking, Google Distance Matrix API bills per element ($N \text{ jobs} \times M \text{ techs}$), quickly scaling into thousands of dollars monthly.
> 3. **Reliability & Testability**: External network dependencies break deterministic unit tests and create external points of failure.
>
> By utilizing the mathematical Haversine formula in pure Java, spatial distance is calculated in sub-microsecond $O(1)$ time in-memory. This acts as an instantaneous spatial pre-filter. In a full production rollout, Haversine narrows down the top 3–5 candidate pool, after which turn-by-turn road network APIs can be queried strictly for the finalists."

---

### Q7: Explain the candidate scoring formula and its design rationale.
**Authoritative Answer:**
> "The scoring algorithm balances four distinct operational vectors:
> $$\text{Score} = (W_{skill} \cdot S_{skill}) + (W_{workload} \cdot S_{workload}) + (W_{shift} \cdot S_{shift}) - (W_{dist} \cdot D_{km})$$
> * **Skill Matching ($0 \dots 40$ pts)**: Hardest requirement. A technician missing key certifications or trade skills cannot legally or safely execute the job.
> * **Workload Equalization ($0 \dots 25$ pts)**: Penalizes over-utilized technicians to prevent overtime burnout and job delays.
> * **Shift Buffer ($0 \dots 20$ pts)**: Ensures that the technician has sufficient working hours remaining before shift end to complete the job without handing off mid-repair.
> * **Distance Decay ($-0.5 \text{ pts} / \text{km}$)**: Minimizes windshield time and fuel consumption."

---

## 🔄 Section 4: Real-Time Architecture & WebSockets

### Q8: Why did you choose STOMP over raw WebSockets?
**Authoritative Answer:**
> "Raw WebSockets only provide a raw TCP-like byte/text stream; they define no application-level semantics for pub-sub channels, headers, destinations, or message boundaries.
>
> STOMP (Simple Text Oriented Messaging Protocol) introduces a standardized framing protocol over WebSocket connections:
> * Standard commands: `CONNECT`, `SUBSCRIBE`, `SEND`, `MESSAGE`, `DISCONNECT`.
> * Destination-based message routing (`/topic/jobs`, `/topic/sla-alerts`).
> * Seamless compatibility with external enterprise brokers (RabbitMQ, ActiveMQ) if horizontal scaling requires replacing Spring's in-memory SimpleBroker."

---

### Q9: How do you handle frontend state synchronization when a STOMP message is received?
**Authoritative Answer:**
> "Instead of mutating local component state directly from the WebSocket payload—which often causes subtle cache desynchronization bugs—we use **TanStack Query (React Query) Cache Invalidation**:
> ```typescript
> case 'JOB_ASSIGNED':
>   queryClient.invalidateQueries({ queryKey: ['jobs'] });
>   queryClient.invalidateQueries({ queryKey: ['technicians'] });
>   break;
> ```
> When an event arrives, TanStack Query immediately marks existing queries as stale and triggers a lightweight background re-fetch. This guarantees that the UI is always perfectly synchronized with the authoritative PostgreSQL database state while still presenting immediate floating toast feedback to the operator."

---

## 🛡 Section 5: Engineering Standards & Toolchain

### Q10: Why did you explicitly forbid Lombok in this codebase?
**Authoritative Answer:**
> "While Lombok saves boilerplate, it relies on internal compiler APIs (`com.sun.tools.javac`) that are private to JDK internals. In modern JDK versions (Java 21, 22, 23, and upcoming LTS releases), strong encapsulation of JDK internals (JEP 396 / JEP 403) frequently causes annotation processor breakage, IDE plugin desynchronization, and brittle build failures.
>
> By using pure standard Java POJOs and modern Java Records, SwiftRouteOS ensures:
> 1. 100% toolchain forward-compatibility across all current and future OpenJDK distributions.
> 2. Zero build-tool compiler plugin friction.
> 3. Completely transparent bytecode that debuggers, profilers, and static analysis tools can inspect without synthetic method interception."
