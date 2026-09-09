# SwiftRouteOS: Intelligent SLA-Aware Field Service Dispatch Platform

SwiftRouteOS is a full-stack Field Service Management (FSM) platform engineered to solve real-world scheduling, dispatch optimization, inventory reservation concurrency, and SLA escalation for equipment maintenance and utility service companies.

---

## Key Engineering Highlights

* **Explainable Dispatch Engine:** Multi-factor scoring formula ($Score = W_{skill}S_{skill} + W_{urgency}S_{urgency} + W_{avail}S_{avail} - W_{dist}D - W_{load}L$) with hard eligibility pre-filtering and manual override audit logging.
* **Deterministic Concurrency Control:** Concurrency-safe parts reservation backed by PostgreSQL pessimistic locking (`SELECT ... FOR UPDATE`), preventing inventory overselling and race conditions.
* **Idempotent SLA Escalation Worker:** Periodic background breach risk checks backed by unique constraints (`(job_id, threshold_stage)`), ensuring zero duplicate notifications across multiple executions.
* **Modern Operations UI:** React 19 + TypeScript + Vite + Tailwind CSS dashboard with interactive OpenStreetMap/Leaflet visualization and real-time STOMP WebSockets.

---

## Quick Start Guide

### Prerequisites
* Java 21 or higher
* Node.js v20 or higher
* PostgreSQL 16+ (or Docker Compose)

### 1. Database & Infrastructure
If using Docker:
```bash
docker compose up -d
```
Or use the local PostgreSQL server on port `5432` with database `swiftroute_db`, user `swiftroute`, password `swiftroute_secret`.

### 2. Backend Service (Spring Boot 3.4)
```bash
cd backend
./mvnw clean spring-boot:run
```
* API Documentation (Swagger UI): `http://localhost:8080/swagger-ui.html`
* Health Endpoint: `http://localhost:8080/actuator/health`

### 3. Frontend Operations UI (React + Vite)
```bash
cd frontend
npm install
npm run dev
```
* Operations Dashboard: `http://localhost:5173`

---

## Default Demo Credentials
| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `password123` |
| Dispatcher | `dispatcher` | `password123` |
| Lead HVAC Tech | `tech_dave` | `password123` |
| Electrical Specialist | `tech_elena` | `password123` |
| Master Plumber | `tech_marcus` | `password123` |
