# CareGrid — Testing & Quality Assurance Guide

CareGrid implements automated multi-level testing to ensure clinical accuracy, transactional safety, and cryptographic security.

---

## 1. Test Suite Architecture

CareGrid automated test suites are partitioned into focused layers:

| Test Class | Focus Area | Framework |
|---|---|---|
| `SecurityAndAuthTest` | BCrypt hashing, JWT generation, signature tamper rejection, RBAC access boundaries | JUnit 5, Spring Security Test |
| `ReferralAndAllocationServiceTest` | Dijkstra route optimization, local admission logic, referral candidate discovery | JUnit 5, Mockito |
| `WaitingQueueAndPriorityTest` | Severity triage sorting (`CRITICAL` $\rightarrow$ `SERIOUS` $\rightarrow$ `MINOR`), FIFO ordering | JUnit 5 |

---

## 2. Running Automated Tests

### 2.1. Backend Tests (Maven)
Execute all test suites from the `backend/` directory:
```bash
mvn test
```

Expected output:
```
[INFO] Running com.hospital.resourceallocation.security.SecurityAndAuthTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.hospital.resourceallocation.service.ReferralAndAllocationServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] Running com.hospital.resourceallocation.service.WaitingQueueAndPriorityTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 2.2. Running a Single Test Class
```bash
mvn test -Dtest=SecurityAndAuthTest
```

### 2.3. Frontend Build Verification
Verify production bundling and JSX/ESLint compliance:
```bash
cd frontend
npm run build
```

---

## 3. Security Test Cases Verified

1. **Password Salt Uniqueness**: Confirms BCrypt generates distinct salted hashes for identical plaintext inputs, preventing rainbow table attacks.
2. **Signature Integrity**: Tampered JWT tokens (altered payload or modified signature bits) are rejected with a signature validation exception.
3. **Admin Scope**: Verifies an `ADMIN` principal can query any hospital ID without exception.
4. **Operator Scope & Multi-Tenancy**: Verifies a `HOSPITAL_OPERATOR` assigned to Hospital 1 is granted access to Hospital 1, but explicitly rejected with `AccessDeniedException` (HTTP 403) when attempting to access Hospital 2 beds, doctors, or patient records.

---

## 4. Algorithmic Test Cases Verified

1. **Dijkstra Shortest Path**: Verifies that when Hospital A is full, the referral engine calculates the lowest-latency path across road junctions and selects the closest eligible facility.
2. **Bed Compatibility Match**: Ensures `CRITICAL` patients are never routed to `GENERAL` beds.
3. **Priority Queue Ordering**: Confirms that a `CRITICAL` patient arriving at $T+10$ minutes preempts a `SERIOUS` patient who arrived at $T+0$ minutes in the waiting queue.
