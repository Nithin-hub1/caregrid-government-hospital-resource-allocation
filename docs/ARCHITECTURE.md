# CareGrid — Architecture & Design Document

## 1. Architectural Overview

CareGrid is architected as a layered full-stack application following clean architecture and Domain-Driven Design (DDD) principles:

```
┌────────────────────────────────────────────────────────┐
│                   Presentation Layer                   │
│           React 19 SPA (Vite, Recharts, Lucide)        │
└───────────────────────────┬────────────────────────────┘
                            │ HTTPS / REST (JWT Bearer)
┌───────────────────────────▼────────────────────────────┐
│                    API Gateway / Web                   │
│         Spring Security 6 Stateless Filter Chain       │
│               REST Controllers & Validation            │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                     Service Layer                      │
│   AllocationService · DischargeService · Referral      │
│     WaitingQueueService · DashboardService             │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                   Persistence Layer                    │
│           Spring Data JPA & Hibernate 6.x              │
│       Pessimistic Locking & Transaction Management     │
└───────────────────────────┬────────────────────────────┘
                            │ JDBC
┌───────────────────────────▼────────────────────────────┐
│                     Database Layer                     │
│                  MySQL 9.x Relational DB               │
└────────────────────────────────────────────────────────┘
```

---

## 2. Security Architecture

### Authentication & Authorization Pipeline
1. **Client Request**: Client transmits credentials (`usernameOrEmail`, `password`) to `POST /api/auth/login`.
2. **DaoAuthenticationProvider**: Spring Security authenticates credentials using `CustomUserDetailsService` and verifies the password hash with `BCryptPasswordEncoder(12)`.
3. **Token Issuance**: `JwtTokenProvider` generates an HMAC-SHA256 signed JSON Web Token carrying claims:
   - `sub`: Username
   - `userId`: Primary key ID
   - `role`: Role (`ADMIN` or `HOSPITAL_OPERATOR`)
   - `hospitalId`: Associated hospital ID (null for Admin)
   - `hospitalName`: Associated hospital display name
4. **Stateless Request Interception**: `JwtAuthenticationFilter` validates the bearer token signature and expiration on each inbound request. Valid tokens populate the `SecurityContextHolder` with a `UserPrincipal`.
5. **Authorization Enforcement**: Handled via URL-level pattern matching in `SecurityConfig` and method-level programmatic boundary enforcement in `SecurityUtils.validateHospitalAccess(targetHospitalId)`.

---

## 3. Core Algorithms

### 3.1. Dijkstra Shortest-Path Referral Routing
When a facility exhausts local bed or doctor capacity, `ReferralService` uses Dijkstra's algorithm to identify the optimal referral hospital:
- **Graph Representation**: Hospitals and network junction locations are represented as graph nodes; inter-hospital road segments and transit routes are represented as directed weighted edges (weight = transit time in minutes).
- **Execution**:
  1. A min-priority queue (`PriorityQueue<QueueNode>`) is initialized with source distance 0.
  2. Relax edges iteratively to calculate the shortest path to all network facilities.
  3. Filter destination nodes to those with an active hospital possessing an available matching bed and doctor.
  4. Select the candidate minimizing travel time.

### 3.2. Severity-Based Waiting Queue
Patients waiting for bed admission are ordered via `PatientQueryController` and `WaitingQueueService` using a multi-attribute comparator:
```java
Comparator.comparingInt((Patient p) -> severityRank(p.getSeverity()))
          .thenComparing(Patient::getArrivalTime, Comparator.nullsLast(Comparator.naturalOrder()))
          .thenComparing(Patient::getId)
```
- `CRITICAL` (Severity rank 1)
- `SERIOUS` (Severity rank 2)
- `MINOR` (Severity rank 3)
- First-In-First-Out (FIFO) arrival order for equal severities.

---

## 4. Concurrency & Data Consistency

To prevent double-allocation of beds or concurrent doctor conflicts:
- Critical read-and-assign operations utilize JPA pessimistic write locking (`@Lock(LockModeType.PESSIMISTIC_WRITE)`).
- `patientRepository.findByIdForUpdate(patientId)`
- `bedRepository.findAvailableBedsByHospitalForUpdate(hospitalId, bedType)`
- `doctorRepository.findAvailableDoctorsForUpdate(hospitalId)`
All resource mutations are wrapped in declarative transactions (`@Transactional`), ensuring atomicity across patient status updates, bed occupancy changes, and doctor assignment states.
