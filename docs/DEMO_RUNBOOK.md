# SwiftRouteOS: 5-Minute Interactive Live Demo Runbook

This runbook provides an end-to-end script for live stakeholder demonstrations, technical interviews, and executive showcases.

---

## 📋 Pre-Demo Verification Checklist

1. **Verify Services Running**:
   - Backend API responds: `curl http://localhost:8080/actuator/health` (HTTP 200 `{"status":"UP"}`)
   - Frontend is loaded: Open [http://localhost:5173](http://localhost:5173) (or port 80/3000 if Docker Compose)
   - Database has seed data: 5 users, 3 technicians, 5 inventory items, pre-seeded jobs.
2. **Observe UI Header**:
   - Verify green pulsing pill: `Live STOMP: Connected`
   - Active Persona: Dispatcher Sarah (`dispatcher`)

---

## 🎬 5-Minute Demonstration Script

### Act 1: The Operations Control Center (60 Seconds)
1. **Persona Selection**:
   - Point to the Persona Switcher in the top navigation bar.
   - Note the seamless 1-click role transition between `Dispatcher Sarah`, `Lead Tech Dave`, and `Admin Alex` backed by JWT claims.
2. **Dispatch Board Overview**:
   - Show the interactive OpenStreetMap/Leaflet map rendering customer locations and technician real-time coordinates.
   - Highlight the Live SLA Ticker in the header showing active tickets, approaching breaches, and critical statuses.

---

### Act 2: Explainable Dispatch & Mandatory Override Governance (90 Seconds)
1. **Open Unassigned Job**:
   - Navigate to the **Dispatch Board** or **Job Management** table.
   - Click on an unassigned job (e.g., Job #1: *Commercial Rooftop HVAC Overhaul*).
2. **Review Explainable Recommendations**:
   - Click **"Find Best Technician"**.
   - Show the **Candidate Scoring Modal**:
     - Walk through the scoring breakdown: `Skill Match (+40)`, `Low Workload (+25)`, `Shift Window (+20)`, `Distance Penalty (-3.2)`.
     - Point out how Haversine spherical math calculates spatial proximity in $O(1)$ time.
3. **Trigger Mandatory Override**:
   - Instead of selecting the #1 ranked technician (Dave Miller), select #2 (Elena Rostova).
   - Show how the confirmation modal dynamically marks this as an **OVERRIDE** and disables the submit button until a mandatory justification reason is provided.
   - Type reason: `"Customer explicitly requested Elena due to prior contract familiarity"`.
   - Click **Confirm Dispatch**.
   - Notice the instant WebSocket alert notification and the update in the audit trail.

---

### Act 3: Job Lifecycle State Machine (60 Seconds)
1. **Advance Ticket Lifecycle**:
   - Open the dispatched job drawer.
   - Advance status: `ASSIGNED` $\rightarrow$ `EN_ROUTE` $\rightarrow$ `IN_PROGRESS`.
   - Point out how `actual_response_at` is timestamped upon reaching `IN_PROGRESS`.
2. **Complete & Automatically Consume Inventory**:
   - Progress to `COMPLETED`.
   - Show how reserved parts are consumed permanently from stock, and resolution SLA metrics are recorded.

---

### Act 4: Concurrency Chaos Lab & Row Locking Demonstration (90 Seconds)
1. **Navigate to Chaos Lab**:
   - Click **Chaos Lab** in the navigation sidebar.
2. **Inspect Contended Inventory Record**:
   - Point to item `SCARCE-SENSOR-CHILLER`.
   - Show that **Quantity on Hand = 1**, **Allocated = 0**.
3. **Execute 10-Thread Concurrent Race**:
   - Select **10 Concurrent Threads**.
   - Click **"Run Chaos Simulation"**.
4. **Observe the Live Lock Visualizer**:
   - Watch the central PostgreSQL Engine Lock transition to `HELD (EXCLUSIVE)`.
   - Note the Thread Race Arena:
     - Exactly **1 Thread** secures the row lock $\rightarrow$ returns **HTTP 201 Created**.
     - Exactly **9 Threads** are rejected $\rightarrow$ return **HTTP 409 Conflict Rollback**.
   - Point out that **Quantity on Hand remains 1** and **Allocated becomes 1**—proving the zero oversell invariant under extreme race conditions.

---

### Act 5: Real-Time SLA Escalation Sweep (60 Seconds)
1. **Switch to Admin Persona**:
   - Select **Admin Alex** from the persona switcher.
2. **Trigger Immediate SLA Evaluation**:
   - Navigate to the **SLA & Escalations** view.
   - Click **"Trigger Evaluation Sweep"** (`POST /api/sla/evaluate-now`).
3. **Observe Floating Toast**:
   - Notice the immediate glowing rose alert toast for any breached jobs broadcast across `/topic/sla-alerts`.
   - Explain the database composite unique constraint `(job_id, threshold_stage)` that prevents duplicate alerts regardless of how many times the sweep is triggered.

---

## 🎯 Summary Takeaways for Reviewers
* **Production-Hardened Concurrency**: Database row locks prevent real-world overselling.
* **Deterministic Scoring**: Explainable decisions build operator trust.
* **Resilient Infrastructure**: Spring Boot 3.4, Java 21, React 19, Docker Compose, and zero Lombok annotations.
