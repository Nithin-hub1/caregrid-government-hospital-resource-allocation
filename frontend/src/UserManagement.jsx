import { useEffect, useState } from "react";
import {
  Users,
  UserPlus,
  ShieldCheck,
  Building2,
  RefreshCw,
  Search,
  CheckCircle2,
  AlertCircle,
  X,
  Lock,
  Mail,
  UserCheck,
  UserX,
} from "lucide-react";
import { authFetch } from "./api";

export default function UserManagement() {
  const [users, setUsers] = useState([]);
  const [hospitals, setHospitals] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [search, setSearch] = useState("");
  const [showModal, setShowModal] = useState(false);
  const [busy, setBusy] = useState(false);

  const [form, setForm] = useState({
    username: "",
    email: "",
    password: "",
    role: "HOSPITAL_OPERATOR",
    hospitalId: "",
    fullName: "",
  });

  async function loadData() {
    setLoading(true);
    setError("");
    try {
      const [usersRes, hospitalsRes] = await Promise.all([
        authFetch("/api/users"),
        authFetch("/api/hospitals"),
      ]);

      if (!usersRes.ok) throw new Error("Failed to load user accounts");
      if (!hospitalsRes.ok) throw new Error("Failed to load hospitals");

      const usersData = await usersRes.json();
      const hospitalsData = await hospitalsRes.json();

      setUsers(Array.isArray(usersData) ? usersData : []);
      setHospitals(Array.isArray(hospitalsData) ? hospitalsData : []);
    } catch (err) {
      setError(err.message || "Failed to load data");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  async function handleCreateUser(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    setSuccess("");

    if (!form.username.trim() || !form.email.trim() || !form.password) {
      setError("Username, email, and password are required.");
      setBusy(false);
      return;
    }

    if (form.role === "HOSPITAL_OPERATOR" && !form.hospitalId) {
      setError("Please designate a hospital for the Hospital Operator.");
      setBusy(false);
      return;
    }

    try {
      const payload = {
        username: form.username.trim(),
        email: form.email.trim(),
        password: form.password,
        role: form.role,
        fullName: form.fullName.trim() || form.username.trim(),
        hospitalId: form.role === "HOSPITAL_OPERATOR" ? Number(form.hospitalId) : null,
      };

      const res = await authFetch("/api/users", {
        method: "POST",
        body: JSON.stringify(payload),
      });

      const data = await res.json().catch(() => ({}));

      if (!res.ok) {
        throw new Error(data.error || "Failed to create user account");
      }

      setSuccess(`User "${data.username}" created successfully with role ${data.role}.`);
      setShowModal(false);
      setForm({
        username: "",
        email: "",
        password: "",
        role: "HOSPITAL_OPERATOR",
        hospitalId: "",
        fullName: "",
      });
      await loadData();
    } catch (err) {
      setError(err.message || "Failed to create user");
    } finally {
      setBusy(false);
    }
  }

  async function handleToggleStatus(userId, currentStatus) {
    try {
      const res = await authFetch(`/api/users/${userId}/status`, {
        method: "PUT",
        body: JSON.stringify({ active: !currentStatus }),
      });

      if (!res.ok) throw new Error("Failed to update status");
      await loadData();
    } catch (err) {
      setError(err.message || "Failed to update status");
    }
  }

  const filteredUsers = users.filter((u) => {
    const q = search.toLowerCase();
    return (
      u.username?.toLowerCase().includes(q) ||
      u.email?.toLowerCase().includes(q) ||
      u.fullName?.toLowerCase().includes(q) ||
      u.role?.toLowerCase().includes(q) ||
      u.hospitalName?.toLowerCase().includes(q)
    );
  });

  return (
    <div className="user-management">
      <div className="welcome-row">
        <div>
          <p className="eyebrow">SECURITY & ACCESS CONTROL</p>
          <h2>User Accounts & Role Management</h2>
          <p className="muted">
            Administer system access, assign roles, and bind operators to specific hospitals.
          </p>
        </div>

        <div style={{ display: "flex", gap: "10px" }}>
          <button className="refresh-button" onClick={loadData} disabled={loading}>
            <RefreshCw size={15} />
            Refresh
          </button>
          <button
            className="refresh-button"
            style={{ background: "#159a83", color: "#fff", borderColor: "#159a83" }}
            onClick={() => setShowModal(true)}
          >
            <UserPlus size={16} />
            Create User Account
          </button>
        </div>
      </div>

      {error && (
        <div className="error-banner" style={{ marginBottom: "18px" }}>
          <AlertCircle size={18} />
          <span>{error}</span>
          <button onClick={() => setError("")}>Dismiss</button>
        </div>
      )}

      {success && (
        <div
          style={{
            background: "#edfcf6",
            border: "1px solid #c2eedc",
            color: "#116b5b",
            padding: "12px 16px",
            borderRadius: "8px",
            marginBottom: "18px",
            display: "flex",
            alignItems: "center",
            gap: "9px",
            fontSize: "13px",
          }}
        >
          <CheckCircle2 size={18} />
          <span>{success}</span>
        </div>
      )}

      <div className="panel" style={{ padding: "18px 20px" }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: "16px" }}>
          <div style={{ position: "relative", width: "300px" }}>
            <Search size={16} style={{ position: "absolute", left: "12px", top: "11px", color: "#8ca0af" }} />
            <input
              type="text"
              placeholder="Search user by name, email, role..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{
                width: "100%",
                padding: "9px 12px 9px 36px",
                border: "1px solid #d7e0e7",
                borderRadius: "8px",
                fontSize: "13px",
              }}
            />
          </div>
          <span style={{ fontSize: "12px", color: "#748695" }}>
            Total Registered Users: <strong>{users.length}</strong>
          </span>
        </div>

        <div className="table-scroll">
          <table className="patient-table">
            <thead>
              <tr>
                <th>User</th>
                <th>Email</th>
                <th>Role</th>
                <th>Hospital Association</th>
                <th>Status</th>
                <th>Created At</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: "center", padding: "30px", color: "#8a9aa8" }}>
                    No users matching criteria.
                  </td>
                </tr>
              ) : (
                filteredUsers.map((u) => (
                  <tr key={u.id}>
                    <td>
                      <div className="patient-cell">
                        <div
                          className="patient-avatar"
                          style={{
                            background: u.role === "ADMIN" ? "#e7f7f2" : "#edf4ff",
                            color: u.role === "ADMIN" ? "#159a83" : "#3570bd",
                          }}
                        >
                          {(u.fullName || u.username)[0].toUpperCase()}
                        </div>
                        <div>
                          <strong>{u.fullName || u.username}</strong>
                          <span>@{u.username}</span>
                        </div>
                      </div>
                    </td>
                    <td>{u.email}</td>
                    <td>
                      <span
                        className="status"
                        style={{
                          background: u.role === "ADMIN" ? "#102b3a" : "#e6f4ff",
                          color: u.role === "ADMIN" ? "#49dfc3" : "#0f6cbd",
                          fontSize: "10px",
                          fontWeight: 700,
                        }}
                      >
                        {u.role === "ADMIN" ? "System Admin" : "Hospital Operator"}
                      </span>
                    </td>
                    <td>
                      {u.hospitalName ? (
                        <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                          <Building2 size={15} color="#526779" />
                          <span>{u.hospitalName}</span>
                        </div>
                      ) : (
                        <span style={{ color: "#9ca8b5" }}>System-wide (All Hospitals)</span>
                      )}
                    </td>
                    <td>
                      <span
                        className="status"
                        style={{
                          background: u.active ? "#eaf7ef" : "#fef2f2",
                          color: u.active ? "#2b8254" : "#dc2626",
                        }}
                      >
                        {u.active ? "Active" : "Deactivated"}
                      </span>
                    </td>
                    <td>{u.createdAt ? new Date(u.createdAt).toLocaleDateString() : "—"}</td>
                    <td>
                      <button
                        onClick={() => handleToggleStatus(u.id, u.active)}
                        className="text-button"
                        style={{
                          color: u.active ? "#d9534f" : "#159a83",
                          fontSize: "11px",
                          display: "inline-flex",
                          alignItems: "center",
                          gap: "4px",
                        }}
                      >
                        {u.active ? <UserX size={14} /> : <UserCheck size={14} />}
                        {u.active ? "Deactivate" : "Activate"}
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* CREATE USER MODAL */}
      {showModal && (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(16, 43, 58, 0.6)",
            display: "grid",
            placeItems: "center",
            zIndex: 100,
            padding: "20px",
          }}
        >
          <div
            style={{
              background: "#fff",
              borderRadius: "16px",
              width: "100%",
              maxWidth: "480px",
              boxShadow: "0 20px 40px rgba(0,0,0,0.25)",
              overflow: "hidden",
            }}
          >
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                padding: "20px 24px",
                borderBottom: "1px solid #eef2f5",
                background: "#fbfcfd",
              }}
            >
              <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                <UserPlus size={20} color="#159a83" />
                <h3 style={{ margin: 0, font: "800 16px 'Manrope', sans-serif" }}>
                  Create User Account
                </h3>
              </div>
              <button
                onClick={() => setShowModal(false)}
                style={{ background: "none", border: "none", cursor: "pointer", color: "#8195a4" }}
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleCreateUser} style={{ padding: "24px" }}>
              <div style={{ display: "grid", gap: "16px" }}>
                <div>
                  <label style={{ display: "block", fontSize: "12px", fontWeight: 700, color: "#4f6373", marginBottom: "6px" }}>
                    Full Name
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. Dr. Ramesh Kumar"
                    value={form.fullName}
                    onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                    style={{
                      width: "100%",
                      padding: "10px 12px",
                      border: "1px solid #d4dfe7",
                      borderRadius: "8px",
                      boxSizing: "border-box",
                      fontSize: "13px",
                    }}
                  />
                </div>

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                  <div>
                    <label style={{ display: "block", fontSize: "12px", fontWeight: 700, color: "#4f6373", marginBottom: "6px" }}>
                      Username *
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. operator_erode"
                      value={form.username}
                      onChange={(e) => setForm({ ...form, username: e.target.value })}
                      required
                      style={{
                        width: "100%",
                        padding: "10px 12px",
                        border: "1px solid #d4dfe7",
                        borderRadius: "8px",
                        boxSizing: "border-box",
                        fontSize: "13px",
                      }}
                    />
                  </div>

                  <div>
                    <label style={{ display: "block", fontSize: "12px", fontWeight: 700, color: "#4f6373", marginBottom: "6px" }}>
                      Email Address *
                    </label>
                    <input
                      type="email"
                      placeholder="e.g. operator@caregrid.gov.in"
                      value={form.email}
                      onChange={(e) => setForm({ ...form, email: e.target.value })}
                      required
                      style={{
                        width: "100%",
                        padding: "10px 12px",
                        border: "1px solid #d4dfe7",
                        borderRadius: "8px",
                        boxSizing: "border-box",
                        fontSize: "13px",
                      }}
                    />
                  </div>
                </div>

                <div>
                  <label style={{ display: "block", fontSize: "12px", fontWeight: 700, color: "#4f6373", marginBottom: "6px" }}>
                    Password *
                  </label>
                  <input
                    type="password"
                    placeholder="Minimum 8 characters"
                    value={form.password}
                    onChange={(e) => setForm({ ...form, password: e.target.value })}
                    required
                    style={{
                      width: "100%",
                      padding: "10px 12px",
                      border: "1px solid #d4dfe7",
                      borderRadius: "8px",
                      boxSizing: "border-box",
                      fontSize: "13px",
                    }}
                  />
                </div>

                <div>
                  <label style={{ display: "block", fontSize: "12px", fontWeight: 700, color: "#4f6373", marginBottom: "6px" }}>
                    Role Assignment *
                  </label>
                  <select
                    value={form.role}
                    onChange={(e) => setForm({ ...form, role: e.target.value })}
                    style={{
                      width: "100%",
                      padding: "10px 12px",
                      border: "1px solid #d4dfe7",
                      borderRadius: "8px",
                      boxSizing: "border-box",
                      fontSize: "13px",
                      background: "#fff",
                    }}
                  >
                    <option value="HOSPITAL_OPERATOR">Hospital Operator (Bound to one hospital)</option>
                    <option value="ADMIN">System Administrator (Full network access)</option>
                  </select>
                </div>

                {form.role === "HOSPITAL_OPERATOR" && (
                  <div>
                    <label style={{ display: "block", fontSize: "12px", fontWeight: 700, color: "#4f6373", marginBottom: "6px" }}>
                      Designated Hospital *
                    </label>
                    <select
                      value={form.hospitalId}
                      onChange={(e) => setForm({ ...form, hospitalId: e.target.value })}
                      required
                      style={{
                        width: "100%",
                        padding: "10px 12px",
                        border: "1px solid #d4dfe7",
                        borderRadius: "8px",
                        boxSizing: "border-box",
                        fontSize: "13px",
                        background: "#fff",
                      }}
                    >
                      <option value="">Select a hospital...</option>
                      {hospitals.map((h) => (
                        <option key={h.id} value={h.id}>
                          {h.name} ({h.city}) {h.primaryHospital ? "★ Primary" : ""}
                        </option>
                      ))}
                    </select>
                  </div>
                )}
              </div>

              <div style={{ display: "flex", justifyContent: "flex-end", gap: "10px", marginTop: "24px" }}>
                <button
                  type="button"
                  className="refresh-button"
                  onClick={() => setShowModal(false)}
                  disabled={busy}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="refresh-button"
                  style={{ background: "#159a83", color: "#fff", borderColor: "#159a83" }}
                  disabled={busy}
                >
                  {busy ? "Saving..." : "Create Account"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
