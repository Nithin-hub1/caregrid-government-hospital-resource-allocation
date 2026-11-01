import { useEffect, useState } from "react";
import {
  Building2,
  MapPin,
  Plus,
  RefreshCw,
  Search,
  CheckCircle2,
  AlertCircle,
  X,
  Route,
  Navigation,
} from "lucide-react";
import { authFetch } from "./api";

export default function HospitalsManagement() {
  const [hospitals, setHospitals] = useState([]);
  const [locations, setLocations] = useState([]);
  const [routes, setRoutes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [showModal, setShowModal] = useState(false);
  const [busy, setBusy] = useState(false);

  const [form, setForm] = useState({
    name: "",
    address: "",
    city: "Erode",
    primaryHospital: false,
    networkLocationId: "",
  });

  async function loadData() {
    setLoading(true);
    setError("");
    try {
      const [hospRes, locRes, routeRes] = await Promise.all([
        authFetch("/api/hospitals"),
        authFetch("/api/network-locations").catch(() => ({ ok: false })),
        authFetch("/api/hospital-routes").catch(() => ({ ok: false })),
      ]);

      if (!hospRes.ok) throw new Error("Failed to load hospitals");

      const hospData = await hospRes.json();
      const locData = locRes.ok ? await locRes.json() : [];
      const routeData = routeRes.ok ? await routeRes.json() : [];

      setHospitals(Array.isArray(hospData) ? hospData : []);
      setLocations(Array.isArray(locData) ? locData : []);
      setRoutes(Array.isArray(routeData) ? routeData : []);
    } catch (err) {
      setError(err.message || "Failed to load hospital data");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, []);

  async function handleCreateHospital(e) {
    e.preventDefault();
    setBusy(true);
    setError("");
    setSuccess("");

    if (!form.name.trim() || !form.address.trim()) {
      setError("Hospital name and address are required.");
      setBusy(false);
      return;
    }

    try {
      const payload = {
        name: form.name.trim(),
        address: form.address.trim(),
        city: form.city.trim() || "Erode",
        active: true,
        primaryHospital: form.primaryHospital,
        networkLocation: form.networkLocationId
          ? { id: Number(form.networkLocationId) }
          : null,
      };

      const res = await authFetch("/api/hospitals", {
        method: "POST",
        body: JSON.stringify(payload),
      });

      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.error || "Failed to register hospital");

      setSuccess(`Hospital "${data.name}" registered successfully.`);
      setShowModal(false);
      setForm({
        name: "",
        address: "",
        city: "Erode",
        primaryHospital: false,
        networkLocationId: "",
      });
      await loadData();
    } catch (err) {
      setError(err.message || "Error registering hospital");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <div className="welcome-row">
        <div>
          <p className="eyebrow">FACILITY NETWORK</p>
          <h2>Hospital Network & Facilities</h2>
          <p className="muted">
            Administer government hospitals, network connectivity, and dispatch centers.
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
            <Plus size={16} />
            Register Hospital
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

      <div className="stats-grid" style={{ marginBottom: "22px" }}>
        <article className="stat-card">
          <p className="stat-title">Total Network Hospitals</p>
          <div className="stat-value">{hospitals.length}</div>
          <p className="stat-subtitle">Registered medical facilities</p>
        </article>

        <article className="stat-card">
          <p className="stat-title">Active Facilities</p>
          <div className="stat-value">{hospitals.filter((h) => h.active).length}</div>
          <p className="stat-subtitle">Ready to accept admissions</p>
        </article>

        <article className="stat-card">
          <p className="stat-title">Network Locations</p>
          <div className="stat-value">{locations.length}</div>
          <p className="stat-subtitle">Geographical transit nodes</p>
        </article>

        <article className="stat-card">
          <p className="stat-title">Inter-Hospital Routes</p>
          <div className="stat-value">{routes.length}</div>
          <p className="stat-subtitle">Active Dijkstra transfer paths</p>
        </article>
      </div>

      <div className="panel" style={{ padding: "20px 24px" }}>
        <h3 style={{ margin: "0 0 16px", font: "800 15px 'Manrope', sans-serif" }}>
          Registered Hospital Facilities
        </h3>

        <div className="table-scroll">
          <table className="patient-table">
            <thead>
              <tr>
                <th>Facility Name</th>
                <th>City & Address</th>
                <th>Network Role</th>
                <th>Linked Location</th>
                <th>Operational Status</th>
              </tr>
            </thead>
            <tbody>
              {hospitals.map((h) => (
                <tr key={h.id}>
                  <td>
                    <div className="patient-cell">
                      <div className="patient-avatar" style={{ background: "#e8f7f2", color: "#159a83" }}>
                        <Building2 size={18} />
                      </div>
                      <div>
                        <strong>{h.name}</strong>
                        <span>Hospital ID #{h.id}</span>
                      </div>
                    </div>
                  </td>
                  <td>
                    <div style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                      <MapPin size={14} color="#8c9fae" />
                      <span>{h.address}, {h.city}</span>
                    </div>
                  </td>
                  <td>
                    <span
                      className="status"
                      style={{
                        background: h.primaryHospital ? "#102b3a" : "#edf4ff",
                        color: h.primaryHospital ? "#31c4a7" : "#2f6fb9",
                      }}
                    >
                      {h.primaryHospital ? "★ Primary Facility" : "Referral Hospital"}
                    </span>
                  </td>
                  <td>
                    {h.networkLocation ? (
                      <span style={{ fontSize: "12px", color: "#4f6575" }}>
                        {h.networkLocation.name} ({h.networkLocation.city})
                      </span>
                    ) : (
                      <span style={{ color: "#9ca8b5" }}>—</span>
                    )}
                  </td>
                  <td>
                    <span
                      className="status"
                      style={{
                        background: h.active ? "#eaf7ef" : "#fef2f2",
                        color: h.active ? "#2b8254" : "#dc2626",
                      }}
                    >
                      {h.active ? "Online & Active" : "Inactive"}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* REGISTER HOSPITAL MODAL */}
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
                <Building2 size={20} color="#159a83" />
                <h3 style={{ margin: 0, font: "800 16px 'Manrope', sans-serif" }}>
                  Register New Hospital
                </h3>
              </div>
              <button
                onClick={() => setShowModal(false)}
                style={{ background: "none", border: "none", cursor: "pointer", color: "#8195a4" }}
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleCreateHospital} style={{ padding: "24px" }}>
              <div style={{ display: "grid", gap: "16px" }}>
                <div>
                  <label style={{ display: "block", fontSize: "12px", fontWeight: 700, color: "#4f6373", marginBottom: "6px" }}>
                    Hospital Name *
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. Government Taluk Hospital"
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
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
                    Street Address *
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. Bhavani Main Road, Perundurai"
                    value={form.address}
                    onChange={(e) => setForm({ ...form, address: e.target.value })}
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

                <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "12px" }}>
                  <div>
                    <label style={{ display: "block", fontSize: "12px", fontWeight: 700, color: "#4f6373", marginBottom: "6px" }}>
                      City / Region
                    </label>
                    <input
                      type="text"
                      placeholder="Erode"
                      value={form.city}
                      onChange={(e) => setForm({ ...form, city: e.target.value })}
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
                      Network Location
                    </label>
                    <select
                      value={form.networkLocationId}
                      onChange={(e) => setForm({ ...form, networkLocationId: e.target.value })}
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
                      <option value="">None / Unassigned</option>
                      {locations.map((loc) => (
                        <option key={loc.id} value={loc.id}>
                          {loc.name} ({loc.city})
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div style={{ display: "flex", alignItems: "center", gap: "8px", marginTop: "4px" }}>
                  <input
                    type="checkbox"
                    id="primaryCheck"
                    checked={form.primaryHospital}
                    onChange={(e) => setForm({ ...form, primaryHospital: e.target.checked })}
                  />
                  <label htmlFor="primaryCheck" style={{ fontSize: "13px", color: "#2f4657", cursor: "pointer" }}>
                    Set as Primary Hospital in Resource Allocation Network
                  </label>
                </div>
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
                  {busy ? "Registering..." : "Register Facility"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
