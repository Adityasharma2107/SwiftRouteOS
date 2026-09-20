# SwiftRouteOS: REST API & WebSocket Protocol Specification

## 1. Global API Standards

### Standard Envelope (`ApiResponse<T>`)
Every successful JSON response conforms to the standardized envelope structure:
```json
{
  "success": true,
  "data": { ... },
  "message": "Operation completed successfully",
  "timestamp": "2026-09-20T16:00:00.000Z"
}
```

### RFC 7807 Problem Details for Error Responses
Error conditions (validation failures, optimistic/pessimistic lock rejections, unauthenticated requests) return standard RFC 7807 format:
```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Insufficient stock for inventory item ID: 42. Available: 0, Requested: 1",
  "instance": "/api/inventory/reserve",
  "timestamp": "2026-09-20T16:00:00.000Z"
}
```

---

## 2. Authentication Endpoints

### `POST /api/auth/login`
Authenticates user credentials and generates JWT tokens.
* **Public Access**
* **Request**:
```json
{
  "username": "dispatcher",
  "password": "password123"
}
```
* **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6...",
    "refreshToken": "dGhpcy1pcy1hLXJlZnJlc2gtdG9rZW4...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "user": {
      "id": 2,
      "username": "dispatcher",
      "fullName": "Sarah Chen",
      "email": "sarah.chen@swiftroute.io",
      "role": "DISPATCHER"
    }
  },
  "message": "Authentication successful"
}
```

### `POST /api/auth/refresh`
Refreshes an expired access token using a valid refresh token.
* **Request**:
```json
{
  "refreshToken": "dGhpcy1pcy1hLXJlZnJlc2gtdG9rZW4..."
}
```

---

## 3. Service Request & Job Workflow Endpoints

### `GET /api/jobs`
Lists service jobs with optional filtering.
* **Query Parameters**:
  * `status`: Filter by status (`PENDING`, `ASSIGNED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`)
  * `priority`: Filter by priority (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`)
* **Response (200 OK)**:
```json
{
  "success": true,
  "data": [
    {
      "id": 101,
      "title": "Hospital HVAC Emergency",
      "description": "Primary compressor failed in ICU wing",
      "priority": "CRITICAL",
      "status": "PENDING",
      "customerLatitude": 37.7749,
      "customerLongitude": -122.4194,
      "customerAddress": "100 Hospital Way, San Francisco, CA",
      "customerName": "St. Jude Medical",
      "targetResponseAt": "2026-09-20T17:00:00Z",
      "targetResolutionAt": "2026-09-20T20:00:00Z",
      "requiredSkills": ["HVAC", "CHILLER_REPAIR"],
      "createdAt": "2026-09-20T16:00:00Z"
    }
  ]
}
```

### `POST /api/jobs`
Creates a new customer job ticket, automatically calculating SLA deadlines based on priority policies.
* **Required Roles**: `DISPATCHER`, `ADMIN`
* **Request**:
```json
{
  "title": "Substation Transformer Overheating",
  "description": "Temperature alarm triggered on feeder 4",
  "priority": "HIGH",
  "customerLatitude": 37.7833,
  "customerLongitude": -122.4167,
  "customerAddress": "500 Power Plant Rd",
  "customerName": "Metro Energy",
  "customerPhone": "555-0199",
  "requiredSkillIds": [1, 3]
}
```

### `PATCH /api/jobs/{id}/transition`
Advances the state machine lifecycle for a job.
* **Request**:
```json
{
  "targetStatus": "IN_PROGRESS"
}
```
* **State Machine Rules**:
  * `PENDING` $\rightarrow$ `ASSIGNED` (via Dispatch API)
  * `ASSIGNED` $\rightarrow$ `EN_ROUTE`
  * `EN_ROUTE` $\rightarrow$ `IN_PROGRESS` (sets `actual_response_at` if not already set)
  * `IN_PROGRESS` $\rightarrow$ `COMPLETED` (sets `actual_resolution_at`, consumes reserved parts)
  * Any state $\rightarrow$ `CANCELLED` (releases reserved parts back to inventory)

---

## 4. Intelligent Dispatch & Ranking Endpoints

### `GET /api/dispatch/recommendations/{jobId}`
Computes Haversine spatial distances and multi-factor scores for eligible technicians.
* **Required Roles**: `DISPATCHER`, `ADMIN`
* **Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "jobId": 101,
    "recommendations": [
      {
        "technicianId": 5,
        "name": "Dave Miller",
        "distanceKm": 4.2,
        "matchScore": 88.5,
        "rank": 1,
        "breakdown": {
          "skillScore": 40.0,
          "workloadScore": 25.0,
          "shiftScore": 20.0,
          "distancePenalty": -2.1
        },
        "matchedSkills": ["HVAC", "CHILLER_REPAIR"],
        "missingSkills": []
      },
      {
        "technicianId": 7,
        "name": "Elena Rostova",
        "distanceKm": 12.8,
        "matchScore": 68.6,
        "rank": 2,
        "breakdown": {
          "skillScore": 20.0,
          "workloadScore": 25.0,
          "shiftScore": 20.0,
          "distancePenalty": -6.4
        },
        "matchedSkills": ["HVAC"],
        "missingSkills": ["CHILLER_REPAIR"]
      }
    ]
  }
}
```

### `POST /api/dispatch/confirm`
Assigns the selected technician with pessimistic validation. If overriding the top-ranked candidate, `overrideReason` is mandatory.
* **Request**:
```json
{
  "jobId": 101,
  "selectedTechnicianId": 7,
  "scheduledStartTime": "2026-09-20T17:00:00Z",
  "scheduledEndTime": "2026-09-20T19:00:00Z",
  "overrideReason": "Customer explicitly requested Elena due to prior installation familiarity",
  "notes": "Gate code is #4402"
}
```

---

## 5. Inventory Concurrency Endpoints

### `GET /api/inventory`
Retrieves parts catalog, stock levels, and allocated reservations.
* **Response (200 OK)**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "sku": "SCARCE-SENSOR-CHILLER",
      "name": "Industrial Chiller Pressure Transducer",
      "category": "HVAC",
      "quantityOnHand": 1,
      "quantityAllocated": 0,
      "availableQuantity": 1,
      "reorderPoint": 2,
      "unitCost": 450.00
    }
  ]
}
```

### `POST /api/inventory/reserve`
Reserves parts for a specific job under database-level row lock (`SELECT ... FOR UPDATE`).
* **Request**:
```json
{
  "jobId": 101,
  "items": [
    {
      "itemId": 1,
      "quantity": 1
    }
  ]
}
```
* **Response (201 Created)** on success.
* **Response (409 Conflict)** if available stock is insufficient.

---

## 6. Real-Time WebSocket Protocol (STOMP over SockJS)

Clients connect to the WebSocket endpoint at `/ws` using STOMP.

### Subscribable Topics
1. `/topic/jobs`: Job assignments and state transitions.
2. `/topic/sla-alerts`: SLA warnings and breach notifications.
3. `/topic/inventory`: Stock level changes and part allocations.
4. `/topic/chaos`: Real-time multithreaded simulation events.

### Standard Message Frame Payload
```json
{
  "eventType": "SLA_BREACHED",
  "destination": "/topic/sla-alerts",
  "timestamp": "2026-09-20T16:05:00.000Z",
  "message": "SLA BREACHED for Job #101 (Priority: CRITICAL)",
  "payload": {
    "jobId": 101,
    "priority": "CRITICAL",
    "thresholdStage": "BREACHED",
    "targetResolutionAt": "2026-09-20T16:00:00Z",
    "elapsedMinutes": 245
  }
}
```
