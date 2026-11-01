# CareGrid — Database Schema & Data Model Documentation

CareGrid utilizes a normalized relational schema hosted in MySQL 9.x. Persistence is managed via Spring Data JPA and Hibernate.

---

## 1. Entity Relationship Diagram (ERD)

```
┌──────────────────┐           ┌──────────────────┐
│    hospitals     │1        * │      beds        │
├──────────────────┼───────────┼──────────────────┤
│ id (PK)          │           │ id (PK)          │
│ name             │           │ bed_number       │
│ city             │           │ type (ICU/GEN)   │
│ address          │           │ is_available     │
│ is_active        │           │ hospital_id (FK) │
│ primary_hospital │           └──────────────────┘
└────────┬─────────┘                     ▲
         │ 1                             │ 0..1 (assigned_bed_id)
         │                               │
         │ *                   ┌─────────┴────────┐
         ├────────────────────►│     patients     │
         │                     ├──────────────────┤
         │                     │ id (PK)          │
         │                     │ patient_name     │
         │                     │ age              │
         │                     │ gender           │
         │                     │ severity         │
         │                     │ status           │
         │                     │ arrival_time     │
         │                     │ hospital_id (FK) │
         │                     │ assigned_bed_id  │
         │                     └─────────▲────────┘
         │                               │
         │ 1                             │ 0..1 (assigned_patient_id)
         │ *                             │
┌────────┴─────────┐           ┌─────────┴────────┐
│      users       │           │     doctors      │
├──────────────────┤           ├──────────────────┤
│ id (PK)          │           │ id (PK)          │
│ username (UQ)    │           │ name             │
│ email (UQ)       │           │ specialization   │
│ password (BCrypt)│           │ is_available     │
│ role (ADMIN/OP)  │           │ hospital_id (FK) │
│ full_name        │           │ patient_id (FK)  │
│ hospital_id (FK) │           └──────────────────┘
│ is_active        │
└──────────────────┘
```

---

## 2. Table Specifications

### 2.1. `hospitals`
Stores physical hospital facilities in the network.
- `id` (BIGINT, Primary Key, Auto Increment)
- `name` (VARCHAR(255), Not Null)
- `city` (VARCHAR(255), Not Null)
- `address` (VARCHAR(255), Nullable)
- `is_active` (BOOLEAN, Default True)
- `primary_hospital` (BOOLEAN, Default False) — Designates the primary receiving facility.

### 2.2. `beds`
Maintains individual bed records and occupancy status.
- `id` (BIGINT, Primary Key, Auto Increment)
- `bed_number` (VARCHAR(50), Not Null) — Unique identifier within the facility (e.g. `ICU-001`).
- `type` (VARCHAR(20), Not Null) — Permitted values: `ICU`, `GENERAL`.
- `is_available` (BOOLEAN, Default True)
- `hospital_id` (BIGINT, Foreign Key $\rightarrow$ `hospitals.id`, Not Null)

### 2.3. `doctors`
Stores medical practitioners and their active clinical assignments.
- `id` (BIGINT, Primary Key, Auto Increment)
- `name` (VARCHAR(255), Not Null)
- `specialization` (VARCHAR(100), Not Null) — Normalized (e.g. `ICU`, `GENERAL`, `CARDIOLOGY`).
- `is_available` (BOOLEAN, Default True)
- `hospital_id` (BIGINT, Foreign Key $\rightarrow$ `hospitals.id`, Nullable)
- `assigned_patient_id` (BIGINT, Foreign Key $\rightarrow$ `patients.id`, Nullable)

### 2.4. `patients`
Tracks clinical intake, severity triage, and allocation status.
- `id` (BIGINT, Primary Key, Auto Increment)
- `patient_name` (VARCHAR(255), Not Null)
- `age` (INT, Not Null)
- `gender` (VARCHAR(20), Not Null)
- `severity` (VARCHAR(20), Not Null) — `CRITICAL`, `SERIOUS`, `MINOR`.
- `status` (VARCHAR(30), Not Null) — `REGISTERED`, `WAITING`, `ADMITTED`, `REFERRED`, `DISCHARGED`.
- `arrival_time` (DATETIME, Nullable)
- `hospital_id` (BIGINT, Foreign Key $\rightarrow$ `hospitals.id`, Nullable) — Owning/admitting facility.
- `assigned_bed_id` (BIGINT, Nullable)

### 2.5. `users`
System user accounts and access credentials.
- `id` (BIGINT, Primary Key, Auto Increment)
- `username` (VARCHAR(100), Unique, Not Null)
- `email` (VARCHAR(150), Unique, Not Null)
- `password` (VARCHAR(255), Not Null) — BCrypt hashed (Cost factor 12).
- `role` (VARCHAR(50), Not Null) — `ADMIN` or `HOSPITAL_OPERATOR`.
- `full_name` (VARCHAR(150), Nullable)
- `hospital_id` (BIGINT, Foreign Key $\rightarrow$ `hospitals.id`, Nullable) — Associated hospital for operators.
- `is_active` (BOOLEAN, Default True)

### 2.6. `network_locations` & `location_routes` / `hospital_routes`
Topological graph entities storing nodes and directed road transit routes:
- `from_location_id` / `to_location_id` (Foreign Keys)
- `travel_minutes` (INT, Positive) — Weighted transit time used by Dijkstra's algorithm.

---

## 3. Migration & Data Integrity Principles

- **Additive Changes Only**: Schema updates use Hibernate automatic update (`spring.jpa.hibernate.ddl-auto=update`) with additive migrations.
- **Zero Data Loss**: Existing records (`hospitals`, `patients`, `beds`, `doctors`) are preserved across releases.
- **Pessimistic Concurrency**: Row-level locking on `patients`, `beds`, and `doctors` ensures race conditions during high-concurrency admission are completely avoided.
