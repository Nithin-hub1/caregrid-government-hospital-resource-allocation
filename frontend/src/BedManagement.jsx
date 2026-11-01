import { authFetch } from "./api";

import { useCallback, useEffect, useMemo, useState } from "react";
import {
  BedDouble,
  Building2,
  CheckCircle2,
  CircleAlert,
  LoaderCircle,
  Pencil,
  Plus,
  RefreshCw,
  Search,
  X,
} from "lucide-react";

const card = {
  background: "#fff",
  border: "1px solid #e4ebef",
  borderRadius: 14,
  padding: 20,
  minWidth: 0,
};

const inputStyle = {
  width: "100%",
  minWidth: 0,
  boxSizing: "border-box",
  padding: "11px 12px",
  border: "1px solid #d6e0e6",
  borderRadius: 8,
  background: "#fff",
  color: "#172b3a",
  fontSize: 14,
};

const labelStyle = {
  display: "grid",
  gap: 7,
  fontSize: 13,
  fontWeight: 650,
  color: "#526575",
};

const primaryButton = {
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: 8,
  padding: "10px 14px",
  border: "1px solid #087f8c",
  borderRadius: 9,
  background: "#087f8c",
  color: "#fff",
  fontWeight: 650,
  cursor: "pointer",
};

const secondaryButton = {
  display: "inline-flex",
  alignItems: "center",
  justifyContent: "center",
  gap: 7,
  padding: "9px 12px",
  border: "1px solid #d9e2e8",
  borderRadius: 8,
  background: "#fff",
  color: "#243b4a",
  fontWeight: 600,
  cursor: "pointer",
};

async function apiRequest(url, options = {}) {
  const response = await authFetch(url, {
    ...options,
    headers: {
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...options.headers,
    },
  });

  const text = await response.text();
  let data = null;

  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = text;
  }

  if (!response.ok) {
    throw new Error(
      typeof data === "string" && data
        ? data
        : data?.message || data?.error || `Request failed (${response.status})`
    );
  }

  return data;
}

function Status({ available }) {
  const color = available ? "#16804a" : "#b54708";
  const background = available ? "#eaf8ef" : "#fff3e5";

  return (
    <span
      style={{
        display: "inline-flex",
        alignItems: "center",
        gap: 6,
        borderRadius: 30,
        padding: "6px 10px",
        color,
        background,
        fontWeight: 700,
        fontSize: 12,
      }}
    >
      <span
        style={{
          width: 7,
          height: 7,
          borderRadius: "50%",
          background: color,
        }}
      />
      {available ? "Available" : "Occupied"}
    </span>
  );
}

function StatCard({ title, value, subtitle, color }) {
  return (
    <article style={{ ...card, padding: 19 }}>
      <p style={{ margin: "0 0 10px", color: "#718096", fontSize: 13 }}>
        {title}
      </p>
      <div style={{ fontSize: 29, fontWeight: 750, color: color || "#172b3a" }}>
        {value}
      </div>
      <p style={{ margin: "7px 0 0", color: "#718096", fontSize: 13 }}>
        {subtitle}
      </p>
    </article>
  );
}

export default function BedManagement({ currentUser }) {
  const [hospitals, setHospitals] = useState([]);
  const [beds, setBeds] = useState([]);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");

  const [search, setSearch] = useState("");
  const [hospitalFilter, setHospitalFilter] = useState("all");
  const [typeFilter, setTypeFilter] = useState("all");
  const [statusFilter, setStatusFilter] = useState("all");

  const [bedNumber, setBedNumber] = useState("");
  const [bedType, setBedType] = useState("GENERAL");
  const [newHospitalId, setNewHospitalId] = useState("");

  const [editingBed, setEditingBed] = useState(null);
  const [editBedNumber, setEditBedNumber] = useState("");
  const [editBedType, setEditBedType] = useState("GENERAL");
  const [editHospitalId, setEditHospitalId] = useState("");

  const [hospitalName, setHospitalName] = useState("");
  const [hospitalAddress, setHospitalAddress] = useState("");
  const [hospitalCity, setHospitalCity] = useState("");

  const loadData = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const hospitalData = await apiRequest("/api/hospitals");
      const hospitalList = Array.isArray(hospitalData) ? hospitalData : [];

      const bedLists = await Promise.all(
        hospitalList.map(async (hospital) => {
          const result = await apiRequest(
            `/api/beds/hospital/${hospital.id}`
          );
          return Array.isArray(result) ? result : [];
        })
      );

      setHospitals(hospitalList);
      setBeds(bedLists.flat());

      setNewHospitalId((current) =>
        hospitalList.some((hospital) => String(hospital.id) === current)
          ? current
          : hospitalList.length
            ? String(hospitalList[0].id)
            : ""
      );
    } catch (err) {
      setError(err.message || "Could not load hospital data.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const filteredBeds = useMemo(() => {
    const query = search.trim().toLowerCase();

    return beds.filter((bed) => {
      const hospital = hospitals.find(
        (item) => Number(item.id) === Number(bed.hospitalId)
      );

      const matchesHospital =
        hospitalFilter === "all" ||
        String(bed.hospitalId) === hospitalFilter;

      const matchesType = typeFilter === "all" || bed.type === typeFilter;

      const matchesStatus =
        statusFilter === "all" ||
        (statusFilter === "available" && bed.available) ||
        (statusFilter === "occupied" && !bed.available);

      const matchesSearch =
        !query ||
        String(bed.bedNumber || "").toLowerCase().includes(query) ||
        String(bed.type || "").toLowerCase().includes(query) ||
        String(bed.hospitalId).includes(query) ||
        String(hospital?.name || "").toLowerCase().includes(query);

      return (
        matchesHospital &&
        matchesType &&
        matchesStatus &&
        matchesSearch
      );
    });
  }, [beds, hospitals, search, hospitalFilter, typeFilter, statusFilter]);

  const availableBeds = beds.filter((bed) => bed.available).length;
  const occupiedBeds = beds.length - availableBeds;
  const icuBeds = beds.filter((bed) => bed.type === "ICU").length;

  function hospitalLabel(id) {
    const hospital = hospitals.find(
      (item) => Number(item.id) === Number(id)
    );

    return hospital
      ? `${hospital.name} · #${hospital.id}`
      : `Hospital #${id}`;
  }

  async function addBed(event) {
    event.preventDefault();
    setError("");
    setNotice("");

    const number = bedNumber.trim();

    if (!number || !newHospitalId) {
      setError("Enter a bed number and select a hospital.");
      return;
    }

    const duplicate = beds.some(
      (bed) =>
        Number(bed.hospitalId) === Number(newHospitalId) &&
        String(bed.bedNumber).toLowerCase() === number.toLowerCase()
    );

    if (duplicate) {
      setError("That bed number already exists in this hospital.");
      return;
    }

    setSaving(true);

    try {
      await apiRequest("/api/beds", {
        method: "POST",
        body: JSON.stringify({
          bedNumber: number,
          type: bedType,
          hospitalId: Number(newHospitalId),
        }),
      });

      setBedNumber("");
      setNotice(`Bed ${number} was added successfully.`);
      await loadData();
    } catch (err) {
      setError(err.message || "Could not add the bed.");
    } finally {
      setSaving(false);
    }
  }

  function startEdit(bed) {
    setError("");
    setNotice("");
    setEditingBed(bed);
    setEditBedNumber(bed.bedNumber);
    setEditBedType(bed.type);
    setEditHospitalId(String(bed.hospitalId));
  }

  async function saveBedEdit(event) {
    event.preventDefault();
    if (!editingBed) return;

    setError("");
    setNotice("");

    const number = editBedNumber.trim();

    if (!number) {
      setError("Bed number cannot be empty.");
      return;
    }

    const duplicate = beds.some(
      (bed) =>
        bed.id !== editingBed.id &&
        Number(bed.hospitalId) === Number(editHospitalId) &&
        String(bed.bedNumber).toLowerCase() === number.toLowerCase()
    );

    if (duplicate) {
      setError("Another bed in this hospital already uses that number.");
      return;
    }

    setSaving(true);

    try {
      await apiRequest(`/api/beds/${editingBed.id}`, {
        method: "PUT",
        body: JSON.stringify({
          bedNumber: number,
          type: editBedType,
          hospitalId: Number(editHospitalId),
        }),
      });

      setNotice(`Bed ${number} was updated successfully.`);
      setEditingBed(null);
      await loadData();
    } catch (err) {
      setError(err.message || "Could not update the bed.");
    } finally {
      setSaving(false);
    }
  }

  async function addHospital(event) {
    event.preventDefault();
    setError("");
    setNotice("");

    const name = hospitalName.trim();
    const address = hospitalAddress.trim();
    const city = hospitalCity.trim();

    if (!name || !address || !city) {
      setError("Enter the hospital name, address, and city.");
      return;
    }

    setSaving(true);

    try {
      const created = await apiRequest("/api/hospitals", {
        method: "POST",
        body: JSON.stringify({
          name,
          address,
          city,
          active: true,
        }),
      });

      setHospitalName("");
      setHospitalAddress("");
      setHospitalCity("");
      setNotice(
        `${created?.name || name} was added successfully${
          created?.id ? ` (Hospital #${created.id})` : ""
        }.`
      );

      await loadData();

      if (created?.id) {
        setNewHospitalId(String(created.id));
      }
    } catch (err) {
      setError(err.message || "Could not register the hospital.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <div style={{ display: "grid", gap: 22 }}>
      <header
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          flexWrap: "wrap",
          gap: 14,
        }}
      >
        <div>
          <h2 style={{ margin: 0, fontSize: 25, fontWeight: 750 }}>
            Bed Management
          </h2>
          <p style={{ margin: "6px 0 0", color: "#718096", fontSize: 14 }}>
            Manage bed inventory and hospital registrations.
          </p>
        </div>

        <button
          type="button"
          style={secondaryButton}
          onClick={loadData}
          disabled={loading}
        >
          <RefreshCw size={16} />
          Refresh
        </button>
      </header>

      {error && (
        <div
          role="alert"
          style={{
            padding: 13,
            borderRadius: 9,
            background: "#fff0ef",
            color: "#b42318",
            display: "flex",
            gap: 9,
            alignItems: "center",
          }}
        >
          <CircleAlert size={18} />
          <span>{error}</span>
          <button
            type="button"
            onClick={() => setError("")}
            style={{ marginLeft: "auto", border: 0, background: "none", cursor: "pointer" }}
            aria-label="Dismiss error"
          >
            <X size={16} />
          </button>
        </div>
      )}

      {notice && (
        <div
          role="status"
          style={{
            padding: 13,
            borderRadius: 9,
            background: "#eaf8ef",
            color: "#16804a",
            display: "flex",
            gap: 9,
            alignItems: "center",
          }}
        >
          <CheckCircle2 size={18} />
          <span>{notice}</span>
          <button
            type="button"
            onClick={() => setNotice("")}
            style={{ marginLeft: "auto", border: 0, background: "none", cursor: "pointer" }}
            aria-label="Dismiss message"
          >
            <X size={16} />
          </button>
        </div>
      )}

      <section
        style={{
          display: "grid",
          gridTemplateColumns: "repeat(auto-fit, minmax(165px, 1fr))",
          gap: 14,
        }}
      >
        <StatCard
          title="Total beds"
          value={loading ? "—" : beds.length}
          subtitle="Across all hospitals"
        />
        <StatCard
          title="Available beds"
          value={loading ? "—" : availableBeds}
          subtitle="Ready for allocation"
          color="#16804a"
        />
        <StatCard
          title="Occupied beds"
          value={loading ? "—" : occupiedBeds}
          subtitle="Currently in use"
          color="#b54708"
        />
        <StatCard
          title="ICU beds"
          value={loading ? "—" : icuBeds}
          subtitle="Critical-care capacity"
        />
        <StatCard
          title="Hospitals"
          value={loading ? "—" : hospitals.length}
          subtitle="Registered facilities"
        />
      </section>

      {currentUser?.role === 'HOSPITAL_OPERATOR' && (
    <div style={{ background: '#edf4ff', border: '1px solid #bfdbfe', padding: '14px 18px', borderRadius: 10, marginBottom: 18, color: '#1e40af', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <div>
        <strong>Hospital Operator Active:</strong> {currentUser.hospitalName}
        <p style={{ margin: '4px 0 0', fontSize: 12, color: '#3b82f6' }}>
          Live bed capacity & allocation oversight. Hospital and bed definitions are managed by System Administrators.
        </p>
      </div>
      <span style={{ fontSize: 11, background: '#dbeafe', padding: '4px 10px', borderRadius: 6, fontWeight: 700 }}>
        OPERATOR VIEW
      </span>
    </div>
  )}
  {currentUser?.role !== 'HOSPITAL_OPERATOR' && (
  <>
  {/* ADD HOSPITAL */}
      <section style={card}>
        <div style={{ display: "flex", gap: 10, alignItems: "center", marginBottom: 18 }}>
          <Building2 size={21} color="#087f8c" />
          <div>
            <h3 style={{ margin: 0, fontSize: 17 }}>Add a hospital</h3>
            <p style={{ margin: "5px 0 0", fontSize: 13, color: "#718096" }}>
              Register a new facility in the hospital network.
            </p>
          </div>
        </div>

        <form
          onSubmit={addHospital}
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(170px, 1fr))",
            gap: 14,
            alignItems: "end",
          }}
        >
          <label style={labelStyle}>
            Hospital name
            <input
              style={inputStyle}
              value={hospitalName}
              onChange={(event) => setHospitalName(event.target.value)}
              placeholder="e.g. City General Hospital"
              maxLength={150}
              required
            />
          </label>

          <label style={labelStyle}>
            Address
            <input
              style={inputStyle}
              value={hospitalAddress}
              onChange={(event) => setHospitalAddress(event.target.value)}
              placeholder="Street or road"
              maxLength={250}
              required
            />
          </label>

          <label style={labelStyle}>
            City
            <input
              style={inputStyle}
              value={hospitalCity}
              onChange={(event) => setHospitalCity(event.target.value)}
              placeholder="e.g. Erode"
              maxLength={100}
              required
            />
          </label>

          <button
            type="submit"
            style={primaryButton}
            disabled={saving || loading}
          >
            {saving ? <LoaderCircle size={16} /> : <Plus size={17} />}
            Add hospital
          </button>
        </form>
      </section>

      {/* ADD BED */}
      <section style={card}>
        <div style={{ display: "flex", gap: 10, alignItems: "center", marginBottom: 18 }}>
          <BedDouble size={21} color="#087f8c" />
          <div>
            <h3 style={{ margin: 0, fontSize: 17 }}>Register a new bed</h3>
            <p style={{ margin: "5px 0 0", fontSize: 13, color: "#718096" }}>
              New beds are initially marked as available.
            </p>
          </div>
        </div>

        <form
          onSubmit={addBed}
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))",
            gap: 14,
            alignItems: "end",
          }}
        >
          <label style={labelStyle}>
            Bed number
            <input
              style={inputStyle}
              value={bedNumber}
              onChange={(event) => setBedNumber(event.target.value)}
              placeholder="e.g. ICU-002"
              maxLength={40}
              required
            />
          </label>

          <label style={labelStyle}>
            Bed type
            <select
              style={inputStyle}
              value={bedType}
              onChange={(event) => setBedType(event.target.value)}
            >
              <option value="GENERAL">General</option>
              <option value="ICU">ICU</option>
            </select>
          </label>

          <label style={labelStyle}>
            Hospital
            <select
              style={inputStyle}
              value={newHospitalId}
              onChange={(event) => setNewHospitalId(event.target.value)}
              required
            >
              {hospitals.map((hospital) => (
                <option key={hospital.id} value={hospital.id}>
                  {hospital.name} · #{hospital.id}
                </option>
              ))}
            </select>
          </label>

          <button
            type="submit"
            style={primaryButton}
            disabled={saving || loading || hospitals.length === 0}
          >
            {saving ? <LoaderCircle size={16} /> : <Plus size={17} />}
            Add bed
          </button>
        </form>
      </section>

      </>
  )}
  {/* INVENTORY */}
      <section style={card}>
        <h3 style={{ margin: "0 0 17px", fontSize: 17 }}>
          Hospital bed inventory
        </h3>

        <div
          style={{
            display: "flex",
            gap: 12,
            flexWrap: "wrap",
            alignItems: "center",
            marginBottom: 16,
          }}
        >
          <div style={{ position: "relative", flex: "1 1 190px" }}>
            <Search
              size={16}
              style={{
                position: "absolute",
                left: 11,
                top: 12,
                color: "#718096",
              }}
            />
            <input
              style={{ ...inputStyle, paddingLeft: 35 }}
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search beds or hospitals..."
            />
          </div>

          <select
            aria-label="Filter by hospital"
            style={{ ...inputStyle, width: "auto", flex: "1 1 150px" }}
            value={hospitalFilter}
            onChange={(event) => setHospitalFilter(event.target.value)}
          >
            <option value="all">All hospitals</option>
            {hospitals.map((hospital) => (
              <option key={hospital.id} value={hospital.id}>
                Hospital #{hospital.id}
              </option>
            ))}
          </select>

          <select
            aria-label="Filter by type"
            style={{ ...inputStyle, width: "auto", flex: "1 1 120px" }}
            value={typeFilter}
            onChange={(event) => setTypeFilter(event.target.value)}
          >
            <option value="all">All types</option>
            <option value="ICU">ICU</option>
            <option value="GENERAL">General</option>
          </select>

          <select
            aria-label="Filter by availability"
            style={{ ...inputStyle, width: "auto", flex: "1 1 130px" }}
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
          >
            <option value="all">All statuses</option>
            <option value="available">Available</option>
            <option value="occupied">Occupied</option>
          </select>
        </div>

        {loading ? (
          <div style={{ padding: 30, textAlign: "center", color: "#718096" }}>
            <LoaderCircle size={20} /> Loading inventory...
          </div>
        ) : (
          <div style={{ overflowX: "auto" }}>
            <table style={{ width: "100%", borderCollapse: "collapse", textAlign: "left", fontSize: 14 }}>
              <thead>
                <tr>
                  {["Bed", "Type", "Hospital", "Status", "Actions"].map((heading) => (
                    <th
                      key={heading}
                      style={{
                        padding: "12px 13px",
                        color: "#718096",
                        fontWeight: 650,
                        background: "#f7f9fb",
                        borderBottom: "1px solid #e6edf1",
                        whiteSpace: "nowrap",
                      }}
                    >
                      {heading}
                    </th>
                  ))}
                </tr>
              </thead>

              <tbody>
                {filteredBeds.map((bed) => (
                  <tr key={bed.id}>
                    <td style={{ padding: "14px 13px", borderBottom: "1px solid #edf1f4", whiteSpace: "nowrap" }}>
                      <strong>{bed.bedNumber}</strong>
                      <div style={{ fontSize: 12, color: "#718096", marginTop: 4 }}>
                        Bed ID: {bed.id}
                      </div>
                    </td>

                    <td style={{ padding: "14px 13px", borderBottom: "1px solid #edf1f4" }}>
                      {bed.type === "ICU" ? "ICU" : "General"}
                    </td>

                    <td style={{ padding: "14px 13px", borderBottom: "1px solid #edf1f4", whiteSpace: "nowrap" }}>
                      {hospitalLabel(bed.hospitalId)}
                    </td>

                    <td style={{ padding: "14px 13px", borderBottom: "1px solid #edf1f4" }}>
                      <Status available={bed.available} />
                    </td>

                    <td style={{ padding: "14px 13px", borderBottom: "1px solid #edf1f4" }}>
                      <button
                        type="button"
                        style={secondaryButton}
                        onClick={() => startEdit(bed)}
                      >
                        <Pencil size={15} />
                        Edit
                      </button>
                    </td>
                  </tr>
                ))}

                {filteredBeds.length === 0 && (
                  <tr>
                    <td
                      colSpan={5}
                      style={{ padding: 32, textAlign: "center", color: "#718096" }}
                    >
                      {currentUser?.role === "HOSPITAL_OPERATOR" ? `No beds registered or matching filters for ${currentUser.hospitalName || "this facility"}.` : "No beds match your search or filters."}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        <p style={{ margin: "14px 0 0", color: "#718096", fontSize: 13 }}>
          Showing {filteredBeds.length} of {beds.length} beds.
        </p>
      </section>

      {/* EDIT BED MODAL */}
      {editingBed && (
        <div
          onMouseDown={(event) => {
            if (event.target === event.currentTarget && !saving) {
              setEditingBed(null);
            }
          }}
          style={{
            position: "fixed",
            inset: 0,
            zIndex: 1000,
            background: "rgba(12, 28, 40, 0.55)",
            display: "grid",
            placeItems: "center",
            padding: 16,
          }}
        >
          <section
            role="dialog"
            aria-modal="true"
            aria-labelledby="edit-bed-title"
            style={{
              width: "min(100%, 520px)",
              boxSizing: "border-box",
              background: "#fff",
              borderRadius: 16,
              padding: 24,
              boxShadow: "0 20px 60px rgba(0,0,0,0.2)",
            }}
          >
            <div style={{ display: "flex", justifyContent: "space-between", gap: 12, alignItems: "start", marginBottom: 20 }}>
              <div>
                <h3 id="edit-bed-title" style={{ margin: 0, fontSize: 20 }}>
                  Edit bed
                </h3>
                <p style={{ color: "#718096", fontSize: 13, margin: "6px 0 0" }}>
                  Bed ID: {editingBed.id}
                </p>
              </div>

              <button
                type="button"
                aria-label="Close edit dialog"
                disabled={saving}
                onClick={() => setEditingBed(null)}
                style={secondaryButton}
              >
                <X size={17} />
              </button>
            </div>

            {!editingBed.available && (
              <div
                style={{
                  padding: 12,
                  borderRadius: 8,
                  background: "#fff3e5",
                  color: "#8a4b08",
                  fontSize: 13,
                  marginBottom: 16,
                }}
              >
                This bed is occupied. You can change its number, but its
                hospital and type cannot be changed until it is available.
              </div>
            )}

            <form onSubmit={saveBedEdit} style={{ display: "grid", gap: 16 }}>
              <label style={labelStyle}>
                Bed number
                <input
                  style={inputStyle}
                  value={editBedNumber}
                  onChange={(event) => setEditBedNumber(event.target.value)}
                  maxLength={40}
                  required
                />
              </label>

              <label style={labelStyle}>
                Bed type
                <select
                  style={inputStyle}
                  value={editBedType}
                  disabled={!editingBed.available}
                  onChange={(event) => setEditBedType(event.target.value)}
                >
                  <option value="GENERAL">General</option>
                  <option value="ICU">ICU</option>
                </select>
              </label>

              <label style={labelStyle}>
                Hospital
                <select
                  style={inputStyle}
                  value={editHospitalId}
                  disabled={!editingBed.available}
                  onChange={(event) => setEditHospitalId(event.target.value)}
                  required
                >
                  {hospitals.map((hospital) => (
                    <option key={hospital.id} value={hospital.id}>
                      {hospital.name} · #{hospital.id}
                    </option>
                  ))}
                </select>
              </label>

              <div style={{ display: "flex", justifyContent: "flex-end", gap: 10, flexWrap: "wrap", marginTop: 4 }}>
                <button
                  type="button"
                  style={secondaryButton}
                  disabled={saving}
                  onClick={() => setEditingBed(null)}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  style={primaryButton}
                  disabled={saving}
                >
                  {saving ? <LoaderCircle size={16} /> : <CheckCircle2 size={16} />}
                  {saving ? "Saving..." : "Save changes"}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </div>
  );
}
