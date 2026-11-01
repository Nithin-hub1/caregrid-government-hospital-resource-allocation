import { authFetch } from "./api";

import { useEffect, useMemo, useState } from "react";
import {
  Activity,
  Building2,
  CheckCircle2,
  Edit3,
  RefreshCw,
  Search,
  Stethoscope,
  UserPlus,
  Users,
  X,
} from "lucide-react";

const API = "/api";

const emptyForm = {
  name: "",
  specialization: "",
  hospitalId: "",
};

async function request(url, options = {}) {
  const response = await authFetch(url, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
  });

  const body = await response.json().catch(() => null);

  if (!response.ok) {
    throw new Error(
      body?.message ||
        body?.error ||
        `Request failed (${response.status})`
    );
  }

  return body;
}

export default function DoctorManagement({ currentUser }) {
  const [doctors, setDoctors] = useState([]);
  const [hospitals, setHospitals] = useState([]);
  const [form, setForm] = useState(emptyForm);
  const [editingId, setEditingId] = useState(null);
  const [search, setSearch] = useState("");
  const [hospitalFilter, setHospitalFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [specializationFilter, setSpecializationFilter] =
    useState("ALL");
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  async function loadData() {
    setLoading(true);
    setError("");

    try {
      const [doctorData, hospitalData] = await Promise.all([
        request(`${API}/doctors`),
        request(`${API}/hospitals`),
      ]);

      setDoctors(Array.isArray(doctorData) ? doctorData : []);
      setHospitals(Array.isArray(hospitalData) ? hospitalData : []);
    } catch (err) {
      setError(err.message || "Could not load doctor data.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  const specializations = useMemo(
    () =>
      [...new Set(
        doctors
          .map((doctor) => doctor.specialization)
          .filter(Boolean)
      )].sort(),
    [doctors]
  );

  const filteredDoctors = useMemo(() => {
    const term = search.trim().toLowerCase();

    return doctors.filter((doctor) => {
      const matchesSearch =
        !term ||
        doctor.name?.toLowerCase().includes(term) ||
        doctor.specialization?.toLowerCase().includes(term) ||
        doctor.hospitalName?.toLowerCase().includes(term) ||
        String(doctor.id).includes(term);

      const matchesHospital =
        hospitalFilter === "ALL" ||
        String(doctor.hospitalId) === hospitalFilter;

      const matchesStatus =
        statusFilter === "ALL" ||
        (statusFilter === "AVAILABLE" && doctor.available) ||
        (statusFilter === "ASSIGNED" && !doctor.available);

      const matchesSpecialization =
        specializationFilter === "ALL" ||
        doctor.specialization === specializationFilter;

      return (
        matchesSearch &&
        matchesHospital &&
        matchesStatus &&
        matchesSpecialization
      );
    });
  }, [
    doctors,
    search,
    hospitalFilter,
    statusFilter,
    specializationFilter,
  ]);

  const availableCount = doctors.filter(
    (doctor) => doctor.available
  ).length;

  const assignedCount = doctors.filter(
    (doctor) => doctor.assignedPatientId != null
  ).length;

  function openCreateForm() {
    setEditingId(null);
    setForm({
      ...emptyForm,
      hospitalId: hospitals[0] ? String(hospitals[0].id) : "",
    });
    setError("");
    setSuccess("");
    setShowForm(true);
  }

  function openEditForm(doctor) {
    setEditingId(doctor.id);
    setForm({
      name: doctor.name || "",
      specialization: doctor.specialization || "",
      hospitalId: String(doctor.hospitalId ?? ""),
    });
    setError("");
    setSuccess("");
    setShowForm(true);
  }

  function closeForm() {
    setShowForm(false);
    setEditingId(null);
    setForm(emptyForm);
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setSaving(true);
    setError("");
    setSuccess("");

    try {
      if (!form.name.trim() || !form.specialization.trim()) {
        throw new Error("Enter the doctor's name and specialization.");
      }

      if (!form.hospitalId) {
        throw new Error("Select a hospital.");
      }

      const payload = {
        name: form.name.trim(),
        specialization: form.specialization.trim(),
        hospitalId: Number(form.hospitalId),
      };

      if (editingId != null) {
        await request(`${API}/doctors/${editingId}`, {
          method: "PUT",
          body: JSON.stringify(payload),
        });
        setSuccess("Doctor details updated successfully.");
      } else {
        await request(`${API}/doctors`, {
          method: "POST",
          body: JSON.stringify(payload),
        });
        setSuccess("Doctor added successfully.");
      }

      closeForm();
      await loadData();
    } catch (err) {
      setError(err.message || "Could not save doctor details.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="doctor-management">
      <div className="doctor-page-heading">
        <div>
          <p className="eyebrow">CLINICAL STAFF</p>
          <h2>Doctor Management</h2>
          <p className="muted">
            Manage doctors, specializations, hospital assignments,
            and current availability.
          </p>
        </div>

        <div className="doctor-heading-actions">
          <button
            className="doctor-secondary-button"
            onClick={loadData}
            disabled={loading}
          >
            <RefreshCw size={16} />
            Refresh
          </button>
          <button
            className="doctor-primary-button"
            onClick={openCreateForm}
            disabled={hospitals.length === 0}
          >
            <UserPlus size={17} />
            Add doctor
          </button>
        </div>
      </div>

      {error && (
        <div className="doctor-alert doctor-alert-error" role="alert">
          {error}
          <button
            className="doctor-alert-close"
            onClick={() => setError("")}
            aria-label="Dismiss error"
          >
            <X size={16} />
          </button>
        </div>
      )}

      {success && (
        <div className="doctor-alert doctor-alert-success" role="status">
          <CheckCircle2 size={17} />
          {success}
          <button
            className="doctor-alert-close"
            onClick={() => setSuccess("")}
            aria-label="Dismiss message"
          >
            <X size={16} />
          </button>
        </div>
      )}

      {hospitals.length === 0 && !loading && (
        <div className="doctor-alert doctor-alert-error">
          No hospitals are registered yet. Add a hospital in Bed
          Management before registering a doctor.
        </div>
      )}

      <div className="doctor-stats-grid">
        <article className="doctor-stat-card">
          <div className="doctor-stat-icon doctor-stat-blue">
            <Users size={21} />
          </div>
          <span>Total doctors</span>
          <strong>{doctors.length}</strong>
          <small>Registered in the network</small>
        </article>

        <article className="doctor-stat-card">
          <div className="doctor-stat-icon doctor-stat-green">
            <CheckCircle2 size={21} />
          </div>
          <span>Available doctors</span>
          <strong>{availableCount}</strong>
          <small>Available according to allocation status</small>
        </article>

        <article className="doctor-stat-card">
          <div className="doctor-stat-icon doctor-stat-purple">
            <Activity size={21} />
          </div>
          <span>Assigned to patients</span>
          <strong>{assignedCount}</strong>
          <small>Currently linked to a patient</small>
        </article>

        <article className="doctor-stat-card">
          <div className="doctor-stat-icon doctor-stat-orange">
            <Building2 size={21} />
          </div>
          <span>Hospitals</span>
          <strong>{hospitals.length}</strong>
          <small>Registered locations</small>
        </article>
      </div>

      <section className="doctor-panel">
        <div className="doctor-panel-heading">
          <div>
            <h3>Doctor directory</h3>
            <p>Search and manage registered clinical staff.</p>
          </div>
          <span className="doctor-count-pill">
            {filteredDoctors.length} results
          </span>
        </div>

        <div className="doctor-filters">
          <label className="doctor-search">
            <Search size={17} />
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search name, ID, specialization..."
            />
          </label>

          <select
            aria-label="Filter by hospital"
            value={hospitalFilter}
            onChange={(event) => setHospitalFilter(event.target.value)}
          >
            <option value="ALL">All hospitals</option>
            {hospitals.map((hospital) => (
              <option key={hospital.id} value={String(hospital.id)}>
                {hospital.name} · #{hospital.id}
              </option>
            ))}
          </select>

          <select
            aria-label="Filter by specialization"
            value={specializationFilter}
            onChange={(event) =>
              setSpecializationFilter(event.target.value)
            }
          >
            <option value="ALL">All specializations</option>
            {specializations.map((specialization) => (
              <option key={specialization} value={specialization}>
                {specialization}
              </option>
            ))}
          </select>

          <select
            aria-label="Filter by availability"
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
          >
            <option value="ALL">All statuses</option>
            <option value="AVAILABLE">Available</option>
            <option value="ASSIGNED">Unavailable</option>
          </select>
        </div>

        {loading ? (
          <div className="doctor-empty-state">
            <RefreshCw className="doctor-spin" size={22} />
            <p>Loading doctors from the database...</p>
          </div>
        ) : filteredDoctors.length === 0 ? (
          <div className="doctor-empty-state">
            <Stethoscope size={28} />
            <h3>No doctors found</h3>
            <p>
              Try changing the filters, or add a doctor to the network.
            </p>
          </div>
        ) : (
          <div className="doctor-table-scroll">
            <table className="doctor-table">
              <thead>
                <tr>
                  <th>Doctor</th>
                  <th>Specialization</th>
                  <th>Hospital</th>
                  <th>Availability</th>
                  <th>Patient assignment</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {filteredDoctors.map((doctor) => (
                  <tr key={doctor.id}>
                    <td>
                      <div className="doctor-person-cell">
                        <div className="doctor-avatar">
                          {(doctor.name || "D")
                            .trim()
                            .charAt(0)
                            .toUpperCase()}
                        </div>
                        <div>
                          <strong>{doctor.name}</strong>
                          <small>Doctor #{doctor.id}</small>
                        </div>
                      </div>
                    </td>
                    <td>
                      <span className="doctor-specialization">
                        {doctor.specialization}
                      </span>
                    </td>
                    <td>
                      <div className="doctor-hospital-cell">
                        <Building2 size={15} />
                        <span>{doctor.hospitalName || "Unassigned"}</span>
                      </div>
                    </td>
                    <td>
                      <span
                        className={`doctor-status ${
                          doctor.available ? "is-available" : "is-busy"
                        }`}
                      >
                        <span />
                        {doctor.available ? "Available" : "Unavailable"}
                      </span>
                    </td>
                    <td>
                      {doctor.assignedPatientId != null ? (
                        <span className="doctor-patient-link">
                          Patient #{doctor.assignedPatientId}
                        </span>
                      ) : (
                        <span className="doctor-no-patient">
                          No active assignment
                        </span>
                      )}
                    </td>
                    <td>
                      <button
                        className="doctor-edit-button"
                        onClick={() => openEditForm(doctor)}
                      >
                        <Edit3 size={15} />
                        Edit
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {showForm && (
        <div
          className="doctor-modal-backdrop"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) closeForm();
          }}
        >
          <section
            className="doctor-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="doctor-form-title"
          >
            <div className="doctor-modal-heading">
              <div>
                <p className="eyebrow">
                  {editingId == null ? "NEW STAFF RECORD" : "UPDATE STAFF"}
                </p>
                <h3 id="doctor-form-title">
                  {editingId == null ? "Add a doctor" : "Edit doctor"}
                </h3>
              </div>
              <button
                className="doctor-modal-close"
                onClick={closeForm}
                aria-label="Close form"
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="doctor-form">
              <label>
                Doctor's full name
                <input
                  value={form.name}
                  onChange={(event) =>
                    setForm({ ...form, name: event.target.value })
                  }
                  placeholder="e.g. Dr. Priya Kumar"
                  required
                  maxLength={100}
                />
              </label>

              <label>
                Specialization
                <input
                  value={form.specialization}
                  onChange={(event) =>
                    setForm({
                      ...form,
                      specialization: event.target.value,
                    })
                  }
                  placeholder="e.g. CARDIOLOGY, ICU, GENERAL"
                  required
                  maxLength={100}
                />
              </label>

              <label>
                Hospital
                <select
                  value={form.hospitalId}
                  onChange={(event) =>
                    setForm({ ...form, hospitalId: event.target.value })
                  }
                  disabled={
                    editingId != null &&
                    doctors.find((doctor) => doctor.id === editingId)
                      ?.assignedPatientId != null
                  }
                  required
                >
                  <option value="">Select a hospital</option>
                  {hospitals.map((hospital) => (
                    <option
                      key={hospital.id}
                      value={String(hospital.id)}
                    >
                      {hospital.name} · #{hospital.id}
                    </option>
                  ))}
                </select>
                {editingId != null &&
                  doctors.find((doctor) => doctor.id === editingId)
                    ?.assignedPatientId != null && (
                    <small className="doctor-field-note">
                      Hospital cannot be changed while the doctor is
                      assigned to a patient.
                    </small>
                  )}
              </label>

              {error && (
                <div className="doctor-inline-error" role="alert">
                  {error}
                </div>
              )}

              <div className="doctor-form-actions">
                <button
                  type="button"
                  className="doctor-secondary-button"
                  onClick={closeForm}
                  disabled={saving}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="doctor-primary-button"
                  disabled={saving}
                >
                  {saving
                    ? "Saving..."
                    : editingId == null
                      ? "Save doctor"
                      : "Save changes"}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </div>
  );
}
