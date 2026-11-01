# CareGrid — Government Hospital Resource Allocation System

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)
[![React](https://img.shields.io/badge/React-19-blue.svg)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-8.3-646CFF.svg)](https://vitejs.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-9.7.2-4479A1.svg)](https://www.mysql.com/)
[![Security](https://img.shields.io/badge/Security-Spring%20Security%206%20%2B%20JWT-red.svg)](https://spring.io/projects/spring-security)

CareGrid is an enterprise-grade hospital resource allocation and inter-facility patient transfer coordination system designed for government and regional healthcare networks. It provides real-time oversight of ICU and General beds, doctor assignments, priority-driven patient triage, automated local bed admission, and graph-based Dijkstra shortest-path referral routing across regional hospital locations.

---

## Table of Contents

- [1. Problem Statement & Objectives](#1-problem-statement--objectives)
- [2. System Architecture](#2-system-architecture)
- [3. Key Features](#3-key-features)
- [4. Role-Based Access Control](#4-role-based-access-control)
- [5. Clinical & Operational Workflows](#5-clinical--operational-workflows)
  - [Patient Allocation Lifecycle](#patient-allocation-lifecycle)
  - [Priority Waiting Queue](#priority-waiting-queue)
  - [Dijkstra Referral Routing Engine](#dijkstra-referral-routing-engine)
  - [Discharge & Automatic Reassignment](#discharge--automatic-reassignment)
- [6. Technology Stack](#6-technology-stack)
- [7. Project Directory Structure](#7-project-directory-structure)
- [8. Installation & Setup](#8-installation--setup)
  - [Prerequisites](#prerequisites)
  - [Database Configuration](#database-configuration)
  - [Backend Setup (Spring Boot)](#backend-setup-spring-boot)
  - [Frontend Setup (React + Vite)](#frontend-setup-react--vite)
  - [Original Java Console Application](#original-java-console-application)
- [9. REST API Overview](#9-rest-api-overview)
- [10. Testing & Verification](#10-testing--verification)
- [11. Screenshots](#11-screenshots)
- [12. Future Enhancements](#12-future-enhancements)
- [13. Contributing](#13-contributing)
- [14. License](#14-license)

---

## 1. Problem Statement & Objectives

### The Problem
During surges in critical healthcare demand, public and government hospitals experience severe resource imbalances. Primary facilities frequently exhaust available ICU beds and specialist doctors, leading to bottlenecked emergency queues. Simultaneously, peripheral district and sub-district hospitals may possess vacant beds and available medical personnel, but lack real-time network visibility and algorithmic routing tools to safely coordinate patient transfers.

### Project Objectives
- **Centralized Network Oversight**: Deliver network-wide visibility into bed occupancy, doctor utilization, and waiting queues across multiple government hospitals.
- **Strict Role-Based Data Isolation**: Enforce multi-tenant operational security so hospital operators can only manage their assigned facility while network administrators retain global oversight.
- **Deterministic Resource Allocation**: Automate bed and doctor assignment based on clinical severity triage (`CRITICAL`, `SERIOUS`, `MINOR`).
- **Algorithmic Referral Coordination**: Execute Dijkstra's shortest path algorithm over geographical road network topologies to recommend the nearest hospital with matching beds and doctors.
- **Resource Reutilization**: Automatically admit the highest-priority waiting patient upon patient discharge.

---

## 2. System Architecture

CareGrid is built upon a modern decoupled full-stack architecture:

```
┌────────────────────────────────────────────────────────────────┐
│                   React + Vite SPA Client                     │
│    (Role-Adaptive Dashboard · Resource Ops · Dijkstra Map)    │
└───────────────────────────────┬────────────────────────────────┘
                                │ JSON / REST (Bearer JWT)
┌───────────────────────────────▼────────────────────────────────┐
│               Spring Boot 3.5.16 Application                   │
│                                                                │
│  ┌────────────────────────┐      ┌──────────────────────────┐  │
│  │   Security Filter      │      │    REST Controllers      │  │
│  │  (JWT + BCrypt Auth)   │      │ (Patients, Beds, Refer)  │  │
│  └───────────┬────────────┘      └────────────┬─────────────┘  │
│              │                                │                │
│  ┌───────────▼────────────┐      ┌────────────▼─────────────┐  │
│  │    Business Services   │◄─────┤   Dijkstra Graph Router  │  │
│  │ (Allocation, Discharge)│      │    (Referral Engine)     │  │
│  └───────────┬────────────┘      └──────────────────────────┘  │
│              │                                                 │
│  ┌───────────▼──────────────────────────────────────────────┐  │
│  │           Spring Data JPA & Hibernate 6.x                │  │
│  └───────────────────────────┬──────────────────────────────┘  │
└──────────────────────────────┼─────────────────────────────────┘
                               │ JDBC
┌──────────────────────────────▼─────────────────────────────────┐
│                        MySQL 9.x Database                      │
│ (Hospitals, Beds, Doctors, Patients, Routes, Locations, Users) │
└────────────────────────────────────────────────────────────────┘
```

---

## 3. Key Features

- **Two-Tier Authentication & RBAC**: Stateless JSON Web Token (JWT) authentication backed by BCrypt password hashing.
- **Hospital-Isolated Dashboard**: Operators view isolated statistics for their assigned facility (available ICU beds, General beds, assigned doctors, and admitted/waiting patients), while Admins view aggregate network analytics.
- **Automated Local Bed Allocation**: Evaluates clinical severity (`CRITICAL` $\rightarrow$ `ICU`, `SERIOUS`/`MINOR` $\rightarrow$ `GENERAL`) and assigns matching beds and doctors with optimistic concurrency locking.
- **Dynamic Referral Recommendation**: If a hospital is at capacity, the Dijkstra routing engine scans the road network for reachable facilities with compatible beds and doctors.
- **Continuous Patient Transfer Tracking**: Tracks patient status through `REGISTERED` $\rightarrow$ `ADMITTED` / `WAITING` $\rightarrow$ `REFERRED` $\rightarrow$ `DISCHARGED`.
- **Automatic Queue Backfill**: Discharging a patient immediately triggers the `WaitingQueueService` to allocate the released bed to the highest-severity waiting patient.
- **Preserved Java Console Suite**: Includes the standalone command-line application in `src/` for terminal execution and simulation.

---

## 4. Role-Based Access Control

CareGrid enforces strict authorization boundaries across both backend APIs and the frontend UI:

| Capability | `ADMIN` | `HOSPITAL_OPERATOR` |
|---|:---:|:---:|
| System-wide Analytics Dashboard | Full Network | Own Facility Only |
| Hospital Registration & Network Topology | Read / Write | Read (Public Topology) |
| Bed Inventory Management | Create / Edit / View All | View Own Facility Only |
| Doctor Staffing Management | Create / Edit / View All | View Own Facility Only |
| Patient Registration | Any Hospital | Own Facility Only |
| Patient Bed Allocation | System-wide | Own Facility Only |
| Patient Discharge | System-wide | Own Facility Only |
| Inter-Hospital Referral Coordination | System-wide | Originating / Receiving Facility |
| User Account & Role Management | Create / Assign / Edit | No Access |

---

## 5. Clinical & Operational Workflows

### Patient Allocation Lifecycle
1. **Registration**: An operator registers a patient with name, age, gender, and clinical severity (`CRITICAL`, `SERIOUS`, `MINOR`). The patient is assigned to the operator's hospital.
2. **Allocation Evaluation**:
   - `CRITICAL` patients require an `ICU` bed and an available doctor.
   - `SERIOUS` and `MINOR` patients require a `GENERAL` bed and an available doctor.
3. **Local Admission**: If a compatible bed and doctor exist at the patient's facility, the bed is marked occupied, the doctor is assigned, and the patient status transitions to `ADMITTED`.
4. **Referral Routing**: If local capacity is exhausted, the Dijkstra routing algorithm identifies the nearest reachable hospital with compatible resources and transitions the patient to `REFERRED`.
5. **Waiting Queue**: If no hospital across the entire network has matching capacity, the patient is placed in the priority waiting queue.

### Priority Waiting Queue
Waiting patients are ordered deterministically by:
1. **Severity Rank**: `CRITICAL` (Rank 1) $\rightarrow$ `SERIOUS` (Rank 2) $\rightarrow$ `MINOR` (Rank 3).
2. **Arrival Timestamp**: Patients with identical severity are ordered by arrival time (FIFO).
3. **Patient ID**: Deterministic secondary tie-breaker.

### Dijkstra Referral Routing Engine
The referral service constructs an adjacency graph of connected hospitals and intermediate network locations with weighted travel times (in minutes). It solves for the single-source shortest path to find the fastest eligible hospital:

$$\min_{H \in \text{Eligible}} \left( \text{DijkstraDistance}(\text{Source}, H) \right)$$

### Discharge & Automatic Reassignment
When a patient is discharged via `POST /api/discharges/patients/{id}`:
1. The allocated bed is released (`available = true`).
2. The assigned doctor is unlinked (`available = true`).
3. The patient status transitions to `DISCHARGED`.
4. The system immediately checks the waiting queue: if a waiting patient matches the released bed type, they are automatically admitted and an available doctor is assigned.

---

## 6. Technology Stack

### Backend
- **Language**: Java 25
- **Framework**: Spring Boot 3.5.16
- **Persistence**: Spring Data JPA / Hibernate 6.x
- **Database**: MySQL 9.7.2 (Connector/J)
- **Security**: Spring Security 6, JJWT (io.jsonwebtoken) 0.12.6, BCrypt
- **Build Tool**: Apache Maven

### Frontend
- **Framework**: React 19
- **Build Tool**: Vite 8.3
- **Routing & State**: React Router 7, React Hooks
- **Icons & Visualization**: Lucide React, Recharts
- **Styling**: Responsive CSS3 Design System

---

## 7. Project Directory Structure

```
GovernmentHospitalResourceAllocation/
├── backend/                             # Spring Boot REST API
│   ├── src/main/java/com/hospital/
│   │   └── resourceallocation/
│   │       ├── controller/              # REST Controllers (Auth, Beds, Doctors, Patients, etc.)
│   │       ├── entity/                  # JPA Entities (Hospital, Bed, Doctor, Patient, User, Route)
│   │       ├── repository/              # Spring Data JPA Repositories
│   │       ├── security/                # Spring Security, JWT Token Provider, UserDetailsService
│   │       ├── service/                 # Business Logic (Allocation, Discharge, Referral, Dashboard)
│   │       └── HospitalApplication.java # Spring Boot Main Class
│   ├── src/main/resources/
│   │   ├── application.properties       # Runtime properties (Git ignored for secrets)
│   │   └── application.properties.example # Safe configuration template
│   ├── src/test/java/                   # JUnit 5 integration and unit tests
│   └── pom.xml                          # Maven build definition
│
├── frontend/                            # React + Vite Single Page Application
│   ├── src/
│   │   ├── components/                  # UI Components (UserManagement, HospitalsManagement, etc.)
│   │   ├── App.jsx                      # Main shell, navigation, and overview dashboard
│   │   ├── BedManagement.jsx            # Bed inventory and status tracking
│   │   ├── DoctorManagement.jsx         # Doctor availability management
│   │   ├── PatientManagement.jsx        # Patient intake and allocation controls
│   │   ├── ReferralManagement.jsx       # Dijkstra shortest-path referral coordination
│   │   ├── Login.jsx                    # Secure credential login interface
│   │   └── api.js                       # JWT authenticated fetch client
│   ├── .env.example                     # Environment template
│   ├── package.json                     # Node dependencies and scripts
│   └── vite.config.js                   # Vite configuration with API proxy
│
├── src/                                 # Original Java Console Application
│   ├── Bed.java
│   ├── Doctor.java
│   ├── Hospital.java
│   ├── HospitalNetwork.java
│   ├── HospitalSystem.java              # Interactive console system implementation
│   ├── Main.java                        # Standalone terminal entry point
│   ├── Patient.java
│   ├── PatientPriorityQueue.java
│   ├── ReferralManager.java
│   └── ResourceManager.java
│
├── docs/                                # Detailed Project Documentation
│   ├── ARCHITECTURE.md                  # Comprehensive architectural deep-dive
│   ├── DATABASE.md                      # Schema, relationships, and ER overview
│   ├── API.md                           # REST API endpoint specifications
│   └── TESTING.md                       # Test suites and verification procedures
│
├── .gitignore                           # Comprehensive root ignore rules
└── README.md                            # Main project documentation
```

---

## 8. Installation & Setup

### Prerequisites
- **JDK**: Java 25 OpenJDK or Oracle JDK
- **Build Tool**: Apache Maven 3.9+
- **Node.js**: Node 18+ and npm 9+
- **Database**: MySQL 9.x or 8.x
- **Git**: 2.30+

### Database Configuration
1. Start your local MySQL service.
2. Create the application database:
   ```sql
   CREATE DATABASE hospital_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. Set your environment variables (or copy `backend/src/main/resources/application.properties.example` to `application.properties`):
   ```bash
   export DB_URL="jdbc:mysql://localhost:3306/hospital_db"
   export DB_USERNAME="root"
   export DB_PASSWORD="your_mysql_password"
   export CAREGRID_JWT_SECRET="YourStrongSecretKeyWithAtLeast32Characters2026!"
   ```

### Backend Setup (Spring Boot)
1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Build the project and run tests:
   ```bash
   mvn clean test
   ```
3. Run the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```
   The backend starts on `http://localhost:8080`.
   *Upon initial launch, `DataInitializer` bootstraps default users and safe sample network hospitals.*

### Frontend Setup (React + Vite)
1. Navigate to the frontend directory:
   ```bash
   cd frontend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Launch the development server:
   ```bash
   npm run dev
   ```
   Open your browser to `http://localhost:5173`.

### Original Java Console Application
To run the original command-line hospital allocation system:
```bash
javac src/*.java
java -cp src Main
```

---

## 9. REST API Overview

All API endpoints (except `/api/auth/login` and `/api/health`) require a valid Bearer JWT in the `Authorization` header: `Authorization: Bearer <TOKEN>`.

### Authentication
- `POST /api/auth/login` — Authenticate username/email & password, returns JWT token and user profile.
- `GET /api/auth/me` — Retrieve current authenticated user context.
- `POST /api/auth/logout` — Invalidate client session.

### Dashboard & Analytics
- `GET /api/dashboard/summary` — Scoped overview metrics (Network-wide for Admin, Facility-only for Operator).
- `GET /api/dashboard/hospitals/{hospitalId}` — Facility-specific metrics (Guarded by operator hospital ID).

### Patients
- `POST /api/patients` — Register patient (Automatically associates operator's hospital).
- `GET /api/patients` — List patients (Scoped to operator facility).
- `GET /api/patients/{id}` — Get patient details by ID.
- `GET /api/patients/search` — Filter by name, severity (`CRITICAL`, `SERIOUS`, `MINOR`), and status.
- `GET /api/patients/waiting-queue` — Priority waiting queue ordered by clinical triage.

### Allocations & Discharges
- `POST /api/allocations/patients/{patientId}` — Allocate bed & doctor or queue for referral/waiting.
- `POST /api/discharges/patients/{patientId}` — Discharge patient, release bed, auto-admit next waiting patient.

### Bed & Doctor Management
- `GET /api/beds/hospital/{hospitalId}` — List beds for hospital.
- `POST /api/beds` / `PUT /api/beds/{id}` — Create / modify bed (Admin only).
- `GET /api/doctors` — List doctors (Scoped to operator facility for operators).
- `POST /api/doctors` / `PUT /api/doctors/{id}` — Register / update doctor (Admin only).

### Referral Routing
- `POST /api/referrals/patients/{patientId}?sourceHospitalId={id}` — Compute fastest transfer candidate using Dijkstra.
- `POST /api/referrals/confirm` — Confirm and reserve bed/doctor at target facility.
- `POST /api/referrals/patients/{patientId}/complete-transfer` — Complete transfer upon patient arrival.

*For complete endpoint schemas, parameters, and payloads, consult [docs/API.md](docs/API.md).*

---

## 10. Testing & Verification

### Backend Unit & Integration Tests
Run all automated test suites:
```bash
cd backend
mvn test
```
The test suite covers:
- BCrypt password hashing & salt verification.
- JWT token creation, claim resolution, and tampered signature rejection.
- Spring Security authorization & cross-hospital boundary enforcement.
- Dijkstra algorithm shortest-path calculations.
- Severity waiting queue ordering.

### Frontend Production Build
```bash
cd frontend
npm run build
```
Builds an optimized client bundle into `frontend/dist/`.

---

## 11. Screenshots

> *Placeholder: Add application screenshots here after initial deployment.*

| Overview Dashboard | Patient Management |
|:---:|:---:|
| ![Overview](docs/screenshots/overview.png) | ![Patients](docs/screenshots/patients.png) |

| Bed Inventory | Dijkstra Referral Route |
|:---:|:---:|
| ![Beds](docs/screenshots/beds.png) | ![Referrals](docs/screenshots/referrals.png) |

---

## 12. Future Enhancements

- **Real-Time WebSocket Updates**: Live push notifications for incoming ambulance transfers.
- **GIS Map Visualization**: Interactive OpenStreetMap / Leaflet integration showing ambulance travel routes.
- **Multi-Factor Authentication (MFA)**: TOTP verification for privileged administrative accounts.
- **Audit Logging**: Comprehensive non-repudiation audit trail for bed transitions and patient transfers.

---

## 13. Contributing

Contributions to CareGrid are welcome:
1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/NewFeature`).
3. Commit your changes (`git commit -m 'feat: Add NewFeature'`).
4. Push to the branch (`git push origin feature/NewFeature`).
5. Open a Pull Request.

---

## 14. License

This project is licensed under the terms of the MIT License.
