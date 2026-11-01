# CareGrid — REST API Specification

CareGrid provides a RESTful JSON API. All endpoints (except public authentication and health check) require a valid JWT token passed in the `Authorization` header:

```http
Authorization: Bearer <JWT_TOKEN>
```

---

## 1. Authentication Endpoints

### 1.1. User Login
- **Endpoint**: `POST /api/auth/login`
- **Access**: Public
- **Request Body**:
  ```json
  {
    "usernameOrEmail": "admin",
    "password": "Admin@CareGrid2026!"
  }
  ```
- **Response `200 OK`**:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6...",
    "type": "Bearer",
    "id": 1,
    "username": "admin",
    "email": "admin@caregrid.gov.in",
    "role": "ADMIN",
    "hospitalId": null,
    "hospitalName": null,
    "fullName": "CareGrid System Administrator"
  }
  ```
- **Error Codes**: `400 Bad Request` (validation failure), `401 Unauthorized` (invalid credentials).

### 1.2. Get Current Authenticated Profile
- **Endpoint**: `GET /api/auth/me`
- **Access**: Authenticated (`ADMIN`, `HOSPITAL_OPERATOR`)
- **Response `200 OK`**: Current user details and assigned hospital association.

---

## 2. Dashboard & Analytics

### 2.1. Overview Summary Metrics
- **Endpoint**: `GET /api/dashboard/summary`
- **Access**: Authenticated
- **Behavior**:
  - `ADMIN`: Returns aggregated network-wide counts across all hospitals.
  - `HOSPITAL_OPERATOR`: Automatically returns scoped counts *only* for the operator's assigned hospital.
- **Response `200 OK`**:
  ```json
  {
    "totalHospitals": 1,
    "activeHospitals": 1,
    "totalPatients": 5,
    "registeredPatients": 1,
    "waitingPatients": 1,
    "admittedPatients": 2,
    "dischargedPatients": 1,
    "totalBeds": 3,
    "availableBeds": 1,
    "occupiedBeds": 2,
    "totalIcuBeds": 1,
    "availableIcuBeds": 1,
    "occupiedIcuBeds": 0,
    "totalGeneralBeds": 2,
    "availableGeneralBeds": 0,
    "occupiedGeneralBeds": 2,
    "totalDoctors": 3,
    "availableDoctors": 1,
    "unavailableDoctors": 2,
    "hospitals": [...],
    "scopedHospitalId": 1,
    "scopedHospitalName": "Government General Hospital",
    "operatorView": true
  }
  ```

### 2.2. Facility Metrics by ID
- **Endpoint**: `GET /api/dashboard/hospitals/{hospitalId}`
- **Access**: `ADMIN` or `HOSPITAL_OPERATOR` assigned to that `hospitalId`.
- **Error Codes**: `403 Forbidden` if an operator attempts to access another hospital's metrics.

---

## 3. Patient Management

### 3.1. Register Patient
- **Endpoint**: `POST /api/patients`
- **Access**: Authenticated
- **Request Body**:
  ```json
  {
    "patientName": "Aravind Kumar",
    "age": 42,
    "gender": "Male",
    "severity": "CRITICAL"
  }
  ```
- **Behavior**: When registered by a `HOSPITAL_OPERATOR`, the patient is automatically associated with the operator's hospital.

### 3.2. List Patients
- **Endpoint**: `GET /api/patients`
- **Access**: Authenticated
- **Behavior**: Admins view all patients; operators receive only patients associated with their facility.

### 3.3. Get Patient by ID
- **Endpoint**: `GET /api/patients/{id}`
- **Access**: Authenticated
- **Error Codes**: `403 Forbidden` if operator queries another hospital's private patient; `404 Not Found` if patient does not exist.

### 3.4. Search Patients
- **Endpoint**: `GET /api/patients/search?name={name}&severity={severity}&status={status}`
- **Parameters**:
  - `severity`: `CRITICAL`, `SERIOUS`, `MINOR`
  - `status`: `REGISTERED`, `WAITING`, `ADMITTED`, `REFERRED`, `DISCHARGED`

### 3.5. Priority Waiting Queue
- **Endpoint**: `GET /api/patients/waiting-queue`
- **Access**: Authenticated
- **Behavior**: Returns waiting patients sorted by triage severity (`CRITICAL` first), then arrival timestamp.

---

## 4. Bed Allocation & Discharge

### 4.1. Allocate Patient Bed
- **Endpoint**: `POST /api/allocations/patients/{patientId}`
- **Access**: Authenticated
- **Response `200 OK`**:
  ```json
  {
    "patientId": 10,
    "patientName": "Aravind Kumar",
    "severity": "CRITICAL",
    "status": "ADMITTED",
    "bedId": 1,
    "bedNumber": "ICU-001",
    "bedType": "ICU",
    "hospitalId": 1,
    "doctorId": 3,
    "doctorName": "Dr. K. Senthil",
    "message": "Patient admitted locally to Government General Hospital. Bed ICU-001 and doctor Dr. K. Senthil assigned."
  }
  ```

### 4.2. Discharge Patient
- **Endpoint**: `POST /api/discharges/patients/{patientId}`
- **Access**: Authenticated (Operators can only discharge patients within their hospital).
- **Behavior**: Releases bed and doctor; automatically triggers waiting queue backfill if matching patient is waiting.

---

## 5. Referral Management

### 5.1. Recommend Referral via Dijkstra
- **Endpoint**: `POST /api/referrals/patients/{patientId}?sourceHospitalId={hospitalId}`
- **Access**: Authenticated (Operators can only initiate from their assigned hospital).
- **Response `200 OK`**: Returns recommended hospital, bed, doctor, travel minutes, and full route hops.

### 5.2. Confirm Referral
- **Endpoint**: `POST /api/referrals/confirm`
- **Access**: Authenticated

### 5.3. Complete Transfer
- **Endpoint**: `POST /api/referrals/patients/{patientId}/complete-transfer`
- **Access**: Authenticated (Destination hospital operator or originating operator).

---

## 6. System Health Check

- **Endpoint**: `GET /api/health`
- **Access**: Public
- **Response `200 OK`**:
  ```json
  {
    "status": "UP",
    "system": "CareGrid Resource Allocation System",
    "timestamp": "2026-11-01T20:30:00Z"
  }
  ```
