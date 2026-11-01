import { useState } from "react";
import {
  UserPlus,
  Search,
  BedDouble,
  CheckCircle2,
  AlertCircle,
  LogOut,
  Truck,
} from "lucide-react";
import { authFetch } from "./api";

const API = "/api";

export default function PatientManagement({ patients, onRefresh, currentUser }) {
  const [form, setForm] = useState({
    patientName: "",
    age: "",
    gender: "Male",
    severity: "SERIOUS",
  });

  const [search, setSearch] = useState("");
  const [busy, setBusy] = useState(false);
  const [busyPatientId, setBusyPatientId] = useState(null);
  const [message, setMessage] = useState(null);

  async function request(path, options = {}) {
    const response = await authFetch(`${API}${path}`, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...options.headers,
      },
    });

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
      throw new Error(
        data.error || data.message || `Request failed (${response.status})`
      );
    }

    return data;
  }

  async function registerPatient(event) {
    event.preventDefault();
    setBusy(true);
    setMessage(null);

    try {
      const patient = await request("/patients", {
        method: "POST",
        body: JSON.stringify({
          patientName: form.patientName.trim(),
          age: Number(form.age),
          gender: form.gender,
          severity: form.severity,
        }),
      });

      setMessage({
        type: "success",
        text: `Patient registered successfully${
          patient.id ? ` — ID ${patient.id}` : ""
        }.`,
      });

      setForm({
        patientName: "",
        age: "",
        gender: "Male",
        severity: "SERIOUS",
      });

      await onRefresh();
    } catch (error) {
      setMessage({ type: "error", text: error.message });
    } finally {
      setBusy(false);
    }
  }

  async function allocatePatient(id) {
    setBusy(true);
    setBusyPatientId(id);
    setMessage(null);

    try {
      const result = await request(`/allocations/patients/${id}`, {
        method: "POST",
      });

      setMessage({
        type: "success",
        text:
          result.message ||
          (result.status === "WAITING"
            ? `Patient #${id} is in waiting queue.`
            : `Allocation completed for patient #${id}. Status: ${result.status}`),
      });

      await onRefresh();
    } catch (error) {
      setMessage({ type: "error", text: error.message });
    } finally {
      setBusy(false);
      setBusyPatientId(null);
    }
  }

  async function completeTransfer(patient) {
    setBusy(true);
    setBusyPatientId(patient.id);
    setMessage(null);

    try {
      const result = await request(
        `/referrals/patients/${patient.id}/complete-transfer`,
        { method: "POST" }
      );

      setMessage({
        type: "success",
        text:
          result.message ||
          `Transfer completed! Patient ${patient.patientName} is now admitted at destination hospital.`,
      });

      await onRefresh();
    } catch (error) {
      setMessage({ type: "error", text: error.message });
    } finally {
      setBusy(false);
      setBusyPatientId(null);
    }
  }

  async function dischargePatient(patient) {
    const confirmed = window.confirm(
      `Are you sure you want to discharge ${patient.patientName} (Patient #${patient.id})?\n\nTheir assigned bed and doctor will be released, and the waiting queue will be checked.`
    );

    if (!confirmed) return;

    setBusy(true);
    setBusyPatientId(patient.id);
    setMessage(null);

    try {
      const result = await request(
        `/discharges/patients/${patient.id}`,
        { method: "POST" }
      );

      let text =
        result.message ||
        `Patient ${result.patientName || patient.patientName} discharged successfully.`;

      if (result.releasedBedId != null) {
        text += ` Bed #${result.releasedBedId} released.`;
      }

      if (result.releasedDoctorId != null) {
        text += ` Doctor #${result.releasedDoctorId} released.`;
      }

      if (result.newlyAdmittedPatientId != null) {
        text += ` Patient #${result.newlyAdmittedPatientId} was admitted from the waiting queue.`;
      }

      setMessage({ type: "success", text });

      await onRefresh();
    } catch (error) {
      setMessage({
        type: "error",
        text: `Discharge failed: ${error.message}`,
      });
    } finally {
      setBusy(false);
      setBusyPatientId(null);
    }
  }

  const filtered = patients.filter((patient) => {
    const term = search.trim().toLowerCase();

    return (
      !term ||
      String(patient.id).includes(term) ||
      patient.patientName?.toLowerCase().includes(term) ||
      patient.severity?.toLowerCase().includes(term) ||
      patient.status?.toLowerCase().includes(term)
    );
  });

  return (
    <div className="management-page">
      {currentUser && currentUser.role === "HOSPITAL_OPERATOR" && (
        <div
          style={{
            background: "#eff6ff",
            border: "1px solid #bfdbfe",
            color: "#1e40af",
            padding: "12px 18px",
            borderRadius: "10px",
            marginBottom: "18px",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            fontSize: "13px",
          }}
        >
          <div>
            <strong>Hospital Operator Active:</strong> {currentUser.hospitalName || "Designated Hospital"}
            <span style={{ marginLeft: "10px", color: "#3b82f6" }}>
              (Authorized to admit, allocate, coordinate referrals, and discharge for this facility)
            </span>
          </div>
          <span style={{ fontSize: "11px", background: "#dbeafe", padding: "3px 8px", borderRadius: "5px", fontWeight: 700 }}>
            OPERATOR SESSION
          </span>
        </div>
      )}

      <section className="panel registration-panel">
        <div className="panel-heading">
          <div>
            <h3>Register a patient</h3>
            <p>Create a patient record in the hospital system.</p>
          </div>

          <div className="registration-icon">
            <UserPlus size={22} />
          </div>
        </div>

        <form className="patient-form" onSubmit={registerPatient}>
          <label>
            Patient full name
            <input
              required
              maxLength={120}
              value={form.patientName}
              placeholder="Enter patient's name"
              onChange={(e) =>
                setForm({ ...form, patientName: e.target.value })
              }
            />
          </label>

          <div className="form-row">
            <label>
              Age
              <input
                required
                type="number"
                min="0"
                max="130"
                value={form.age}
                placeholder="Age"
                onChange={(e) =>
                  setForm({ ...form, age: e.target.value })
                }
              />
            </label>

            <label>
              Gender
              <select
                value={form.gender}
                onChange={(e) =>
                  setForm({ ...form, gender: e.target.value })
                }
              >
                <option>Male</option>
                <option>Female</option>
                <option>Other</option>
              </select>
            </label>
          </div>

          <label>
            Medical severity
            <select
              value={form.severity}
              onChange={(e) =>
                setForm({ ...form, severity: e.target.value })
              }
            >
              <option value="CRITICAL">Critical — ICU required</option>
              <option value="SERIOUS">Serious — General bed</option>
              <option value="MINOR">Minor — General bed</option>
            </select>
          </label>

          <button
            className="primary-action"
            disabled={busy}
            type="submit"
          >
            <UserPlus size={17} />
            {busy ? "Processing..." : "Register patient"}
          </button>
        </form>
      </section>

      {message && (
        <div
          className={`form-message ${message.type}`}
          role="status"
          aria-live="polite"
        >
          {message.type === "success" ? (
            <CheckCircle2 size={18} />
          ) : (
            <AlertCircle size={18} />
          )}
          <span>{message.text}</span>
        </div>
      )}

      <section className="panel patient-panel">
        <div className="panel-heading patient-heading">
          <div>
            <h3>Patient records</h3>
            <p>Search, register, allocate and discharge patients.</p>
          </div>

          <div className="search-box">
            <Search size={17} />
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search patients..."
            />
          </div>
        </div>

        <div className="table-scroll">
          <table className="patient-table">
            <thead>
              <tr>
                <th>Patient</th>
                <th>Severity</th>
                <th>Status</th>
                <th>Bed ID</th>
                <th>Action</th>
              </tr>
            </thead>

            <tbody>
              {filtered.map((patient) => (
                <tr key={patient.id}>
                  <td>
                    <div className="patient-cell">
                      <div className="patient-avatar">
                        {(patient.patientName || "P")[0].toUpperCase()}
                      </div>

                      <div>
                        <strong>{patient.patientName}</strong>
                        <span>
                          Patient #{patient.id} · Age {patient.age}
                        </span>
                      </div>
                    </div>
                  </td>

                  <td>
                    <span
                      className={`severity ${(patient.severity || "").toLowerCase()}`}
                    >
                      {patient.severity}
                    </span>
                  </td>

                  <td>
                    <span
                      className={`status ${(patient.status || "").toLowerCase()}`}
                    >
                      {patient.status}
                    </span>
                  </td>

                  <td>{patient.assignedBedId ?? "—"}</td>

                  <td>
                    {["REGISTERED", "WAITING"].includes(patient.status) ? (
                      <button
                        type="button"
                        className="allocate-button"
                        disabled={busy}
                        onClick={() => allocatePatient(patient.id)}
                      >
                        <BedDouble size={15} />
                        {busyPatientId === patient.id
                          ? "Allocating..."
                          : "Allocate"}
                      </button>
                    ) : patient.status === "REFERRED" ? (
                      <div style={{ display: "flex", gap: 6, flexWrap: "nowrap" }}>
                        <button
                          type="button"
                          className="transfer-button"
                          disabled={busy}
                          onClick={() => completeTransfer(patient)}
                          title="Confirm patient arrival and complete transfer"
                        >
                          <Truck size={14} />
                          {busyPatientId === patient.id ? "Completing..." : "Complete Transfer"}
                        </button>
                        <button
                          type="button"
                          className="discharge-button"
                          disabled={busy}
                          onClick={() => dischargePatient(patient)}
                          title="Cancel referral and release resources"
                        >
                          <LogOut size={14} />
                          Release
                        </button>
                      </div>
                    ) : patient.status === "ADMITTED" ? (
                      <button
                        type="button"
                        className="discharge-button"
                        disabled={busy}
                        onClick={() => dischargePatient(patient)}
                      >
                        <LogOut size={15} />
                        {busyPatientId === patient.id
                          ? "Discharging..."
                          : "Discharge"}
                      </button>
                    ) : (
                      <span className="no-action">—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {!filtered.length && (
            <div className="empty-state">No matching patients found.</div>
          )}
        </div>
      </section>
    </div>
  );
}
