import { useEffect, useMemo, useState } from "react";

import {
  Activity,
  BedDouble,
  Building2,
  CalendarDays,
  ChevronRight,
  HeartPulse,
  LayoutDashboard,
  Menu,
  ShieldCheck,
  Stethoscope,
  Users,
  Wifi,
  RefreshCw,
  CheckCircle2,
  LogOut,
  MapPin,
} from "lucide-react";

import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

import Login from "./Login";
import PatientManagement from "./PatientManagement";
import BedManagement from "./BedManagement";
import DoctorManagement from "./DoctorManagement";
import ReferralManagement from "./ReferralManagement";
import HospitalsManagement from "./HospitalsManagement";
import UserManagement from "./UserManagement";
import { authFetch, getUser, getToken, logoutApi } from "./api";

import "./App.css";

const API = "/api";

function StatCard({ title, value, subtitle, icon: Icon, color }) {
  return (
    <article className="stat-card">
      <div className={`stat-icon ${color}`}>
        <Icon size={21} />
      </div>

      <p className="stat-title">{title}</p>
      <div className="stat-value">{value ?? "—"}</div>
      <p className="stat-subtitle">{subtitle}</p>
    </article>
  );
}

export default function App() {
  const [currentUser, setCurrentUser] = useState(() => {
    const token = getToken();
    const user = getUser();
    return token && user ? user : null;
  });

  const [summary, setSummary] = useState(null);
  const [patients, setPatients] = useState([]);
  const [activePage, setActivePage] = useState("Overview");
  const [online, setOnline] = useState(false);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);

  // Listen to 401 unauthorized session expiration events
  useEffect(() => {
    function handleUnauthorized() {
      setCurrentUser(null);
      setError("Your session has expired. Please sign in again.");
    }
    window.addEventListener("caregrid:unauthorized", handleUnauthorized);
    return () => window.removeEventListener("caregrid:unauthorized", handleUnauthorized);
  }, []);

  const navigation = useMemo(() => {
    if (currentUser?.role === "ADMIN") {
      return [
        { label: "Overview", icon: LayoutDashboard },
        { label: "Patients", icon: Users },
        { label: "Hospitals & Network", icon: Building2 },
        { label: "Bed Management", icon: BedDouble },
        { label: "Doctors", icon: Stethoscope },
        { label: "Referrals", icon: Activity },
        { label: "User Accounts", icon: ShieldCheck },
      ];
    }
    // Hospital Operator navigation
    return [
      { label: "Overview", icon: LayoutDashboard },
      { label: "Patients", icon: Users },
      { label: "Bed Management", icon: BedDouble },
      { label: "Doctors", icon: Stethoscope },
      { label: "Referrals", icon: Building2 },
    ];
  }, [currentUser]);

  async function loadData() {
    if (!getToken()) return;
    setLoading(true);

    try {
      const responses = await Promise.all([
        fetch(`${API}/health`),
        authFetch(`${API}/dashboard/summary`),
        authFetch(`${API}/patients`),
      ]);

      if (responses.some((response) => !response.ok)) {
        throw new Error("API request failed");
      }

      const [health, dashboard, patientData] = await Promise.all(
        responses.map((response) => response.json())
      );

      setOnline(health.status === "UP");
      setSummary(dashboard);
      setPatients(Array.isArray(patientData) ? patientData : []);
      setError("");
    } catch (err) {
      console.error("Dashboard loading error:", err);
      setOnline(false);
      setError("Cannot load network data. Please ensure backend services are running.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    if (currentUser) {
      loadData();
    }
  }, [currentUser]);

  async function handleLogout() {
    await logoutApi();
    setCurrentUser(null);
    setSummary(null);
    setPatients([]);
    setActivePage("Overview");
  }

  // If unauthenticated, present the clean CareGrid login interface
  if (!currentUser) {
    return (
      <Login
        onLoginSuccess={(user) => {
          setCurrentUser(user);
          setActivePage("Overview");
        }}
      />
    );
  }

  const bedData = [
    {
      name: "Available",
      value: summary?.availableBeds ?? 0,
      color: "#159a83",
    },
    {
      name: "Occupied",
      value: summary?.occupiedBeds ?? 0,
      color: "#f0a44b",
    },
  ];

  const isOperator = currentUser?.role === "HOSPITAL_OPERATOR";
  const hospitalName = currentUser?.hospitalName || summary?.scopedHospitalName || "Assigned Hospital";

  const operatorBedTypeData = [
    {
      name: "ICU Beds",
      available: summary?.availableIcuBeds ?? 0,
      occupied: summary?.occupiedIcuBeds ?? 0,
    },
    {
      name: "General Beds",
      available: summary?.availableGeneralBeds ?? 0,
      occupied: summary?.occupiedGeneralBeds ?? 0,
    },
  ];

  const hospitalData = (summary?.hospitalResources ?? summary?.hospitals ?? []).map(
    (hospital, index) => ({
      name:
        hospital.hospitalName ??
        hospital.name ??
        `Hospital ${index + 1}`,
      available: hospital.availableBeds ?? 0,
      occupied: hospital.occupiedBeds ?? 0,
    })
  );

  return (
    <div className="app-shell">
      <style>{`
        @media (min-width: 769px) {
          .topbar .mobile-menu {
            display: none !important;
          }
        }

        @media (max-width: 768px) {
          .topbar .mobile-menu {
            display: inline-flex;
            align-items: center;
            justify-content: center;
          }
        }

        .user-role-badge {
          display: inline-block;
          font-size: 9px;
          font-weight: 800;
          padding: 2px 7px;
          border-radius: 4px;
          text-transform: uppercase;
          letter-spacing: 0.5px;
        }

        .role-admin {
          background: #159a83;
          color: #ffffff;
        }

        .role-operator {
          background: #2b70b8;
          color: #ffffff;
        }

        .logout-btn {
          width: 100%;
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 10px 12px;
          margin-top: 12px;
          border: 1px solid #284453;
          border-radius: 8px;
          background: rgba(220, 38, 38, 0.12);
          color: #fca5a5;
          font-size: 12px;
          font-weight: 600;
          transition: all 0.2s;
        }

        .logout-btn:hover {
          background: rgba(220, 38, 38, 0.25);
          color: #fee2e2;
          border-color: #ef4444;
        }

        .operator-facility-banner {
          background: linear-gradient(135deg, #102b3a 0%, #15455d 100%);
          color: white;
          padding: 16px 22px;
          border-radius: 12px;
          margin-bottom: 22px;
          display: flex;
          align-items: center;
          justify-content: space-between;
          box-shadow: 0 4px 14px rgba(16, 43, 58, 0.15);
        }

        .facility-title {
          font-family: 'Manrope', sans-serif;
          font-size: 16px;
          font-weight: 800;
          display: flex;
          align-items: center;
          gap: 8px;
        }
      `}</style>

      <aside className={`sidebar ${menuOpen ? "sidebar-open" : ""}`}>
        <div className="brand">
          <div className="brand-mark">
            <HeartPulse size={26} />
          </div>

          <div>
            <h2>CareGrid</h2>
            <p>Hospital Operations</p>
          </div>
        </div>

        <p className="nav-label">
          {currentUser.role === "ADMIN" ? "ADMIN WORKSPACE" : "OPERATOR WORKSPACE"}
        </p>

        <nav className="navigation">
          {navigation.map(({ label, icon: Icon }) => (
            <button
              key={label}
              className={`nav-item ${
                activePage === label ? "active" : ""
              }`}
              onClick={() => {
                setActivePage(label);
                setMenuOpen(false);
              }}
            >
              <Icon size={19} />
              <span>{label}</span>
              {activePage === label && <ChevronRight size={16} />}
            </button>
          ))}
        </nav>

        <div className="sidebar-bottom">
          <div className="security-card">
            <ShieldCheck size={22} />

            <div>
              <strong>
                {currentUser.role === "ADMIN" ? "System Administration" : "Hospital Operation"}
              </strong>
              <p>
                {currentUser.role === "ADMIN"
                  ? "Full network oversight"
                  : currentUser.hospitalName || "Single hospital operation"}
              </p>
            </div>
          </div>

          <div className="profile">
            <div
              className="avatar"
              style={{
                background: currentUser.role === "ADMIN" ? "#159a83" : "#2b70b8",
                color: "#ffffff",
              }}
            >
              {currentUser.role === "ADMIN" ? "SA" : "OP"}
            </div>

            <div className="profile-text">
              <strong>{currentUser.fullName || currentUser.username}</strong>
              <div style={{ display: "flex", alignItems: "center", gap: 5 }}>
                <span
                  className={`user-role-badge ${
                    currentUser.role === "ADMIN" ? "role-admin" : "role-operator"
                  }`}
                >
                  {currentUser.role === "ADMIN" ? "Admin" : "Operator"}
                </span>
                {currentUser.hospitalName && (
                  <span style={{ fontSize: 9, color: "#8ba5b4", overflow: "hidden", textOverflow: "ellipsis", whiteSpace: "nowrap", maxWidth: 100 }}>
                    {currentUser.hospitalName}
                  </span>
                )}
              </div>
            </div>
          </div>

          <button className="logout-btn" onClick={handleLogout}>
            <LogOut size={15} />
            <span>Sign Out</span>
          </button>
        </div>
      </aside>

      {menuOpen && (
        <button
          className="mobile-overlay"
          aria-label="Close navigation"
          onClick={() => setMenuOpen(false)}
        />
      )}

      <main className="main-area">
        <header className="topbar">
          <div className="topbar-left">
            <button
              className="mobile-menu"
              aria-label="Open navigation"
              onClick={() => setMenuOpen(true)}
            >
              <Menu size={22} />
            </button>

            <div>
              <p className="breadcrumb">
                {currentUser.role === "ADMIN" ? "System Admin" : "Hospital Operator"} / {activePage}
              </p>

              <h1>
                {activePage === "Overview"
                  ? currentUser.role === "ADMIN"
                    ? "Network Overview"
                    : `${currentUser.hospitalName || "Hospital"} Overview`
                  : activePage}
              </h1>
            </div>
          </div>

          <div className="topbar-actions">
            <div className={`connection ${online ? "online" : ""}`}>
              <Wifi size={16} />
              <span>{online ? "System online" : "Connecting..."}</span>
            </div>

            <button
              className="icon-button"
              onClick={loadData}
              disabled={loading}
              aria-label="Refresh data"
              title="Refresh data"
            >
              <RefreshCw size={18} />
            </button>

            <div className="top-date">
              <CalendarDays size={17} />
              <span>{new Date().toLocaleDateString("en-IN")}</span>
            </div>

            <button
              className="icon-button"
              onClick={handleLogout}
              aria-label="Sign out"
              title="Sign out of CareGrid"
              style={{ color: "#d9534f" }}
            >
              <LogOut size={17} />
            </button>
          </div>
        </header>

        <section className="page-content">
          {error && (
            <div className="error-banner">
              {error}
              <button onClick={loadData}>Retry</button>
            </div>
          )}

          {/* FACILITY BANNER FOR HOSPITAL OPERATORS */}
          {currentUser.role === "HOSPITAL_OPERATOR" && currentUser.hospitalName && (
            <div className="operator-facility-banner">
              <div>
                <div className="facility-title">
                  <Building2 size={20} color="#31c4a7" />
                  <span>{currentUser.hospitalName}</span>
                </div>
                <p style={{ margin: "5px 0 0", fontSize: 12, color: "#9fc0d1" }}>
                  Authorized to admit local patients, manage bed allocations, process discharges, and coordinate regional Dijkstra transfers.
                </p>
              </div>
              <div style={{ textAlign: "right", fontSize: 11, color: "#8ab0c3" }}>
                <div>Session: <strong>Active</strong></div>
                <div>Access Level: <strong>Facility Operator</strong></div>
              </div>
            </div>
          )}

          {/* OVERVIEW */}
          {activePage === "Overview" && (
            <>
              <div className="welcome-row">
                <div>
                  <p className="eyebrow">
                    {currentUser.role === "ADMIN"
                      ? "SYSTEM-WIDE SNAPSHOT"
                      : "FACILITY SNAPSHOT"}
                  </p>
                  <h2>
                    Good day, {currentUser.fullName || currentUser.username}.
                  </h2>
                  <p className="muted">
                    {currentUser.role === "ADMIN"
                      ? "Here's the comprehensive live snapshot of your government hospital network."
                      : `Operating overview for ${currentUser.hospitalName || "your medical center"}.`}
                  </p>
                </div>

                <button
                  className="refresh-button"
                  onClick={loadData}
                  disabled={loading}
                >
                  <RefreshCw size={16} />
                  Refresh data
                </button>
              </div>

{/* ROLE-SCOPED DASHBOARD METRICS */}
              {isOperator ? (
                <>
                  <div className="stats-grid">
                    <StatCard
                      title="Available ICU Beds"
                      value={summary?.availableIcuBeds}
                      subtitle={`${summary?.totalIcuBeds ?? 0} total ICU beds`}
                      icon={Activity}
                      color="green"
                    />

                    <StatCard
                      title="Available General Beds"
                      value={summary?.availableGeneralBeds}
                      subtitle={`${summary?.totalGeneralBeds ?? 0} total General beds`}
                      icon={BedDouble}
                      color="blue"
                    />

                    <StatCard
                      title="Occupied Beds"
                      value={summary?.occupiedBeds}
                      subtitle={`${summary?.totalBeds ?? 0} total facility beds`}
                      icon={HeartPulse}
                      color="orange"
                    />

                    <StatCard
                      title="Available Doctors"
                      value={summary?.availableDoctors}
                      subtitle={`${summary?.unavailableDoctors ?? 0} doctors assigned to patients`}
                      icon={Stethoscope}
                      color="purple"
                    />
                  </div>

                  <div className="stats-grid" style={{ marginTop: 14 }}>
                    <StatCard
                      title="Admitted Patients"
                      value={summary?.admittedPatients}
                      subtitle="Currently occupying facility beds"
                      icon={Users}
                      color="blue"
                    />

                    <StatCard
                      title="Waiting Patients"
                      value={summary?.waitingPatients}
                      subtitle="In priority admission queue"
                      icon={RefreshCw}
                      color="orange"
                    />

                    <StatCard
                      title="Discharged Patients"
                      value={summary?.dischargedPatients}
                      subtitle="Associated with this facility"
                      icon={CheckCircle2 || Activity}
                      color="green"
                    />

                    <StatCard
                      title="Total Facility Patients"
                      value={summary?.totalPatients}
                      subtitle="Total registered records"
                      icon={Users}
                      color="purple"
                    />
                  </div>
                </>
              ) : (
                <div className="stats-grid">
                  <StatCard
                    title="Total Patients"
                    value={summary?.totalPatients}
                    subtitle={`${summary?.admittedPatients ?? 0} currently admitted`}
                    icon={Users}
                    color="blue"
                  />

                  <StatCard
                    title="Available Beds"
                    value={summary?.availableBeds}
                    subtitle={`${summary?.totalBeds ?? 0} beds in the network`}
                    icon={BedDouble}
                    color="green"
                  />

                  <StatCard
                    title="Available Doctors"
                    value={summary?.availableDoctors}
                    subtitle={`${summary?.totalDoctors ?? 0} doctors in the network`}
                    icon={Stethoscope}
                    color="purple"
                  />

                  <StatCard
                    title="Active Hospitals"
                    value={summary?.activeHospitals}
                    subtitle={`${summary?.totalHospitals ?? 0} registered hospitals`}
                    icon={Building2}
                    color="orange"
                  />
                </div>
              )}

              <div className="charts-grid">
                <article className="panel">
                  <div className="panel-heading">
                    <div>
                      <h3>{isOperator ? `${hospitalName} — Bed Capacity` : "Hospital resource capacity"}</h3>
                      <p>{isOperator ? "Available vs occupied beds by type (ICU & General)" : "Available vs occupied beds by hospital"}</p>
                    </div>
                    <span className="live-tag">LIVE DATA</span>
                  </div>

                  <div className="chart-area">
                    {(isOperator ? operatorBedTypeData : hospitalData).length > 0 ? (
                      <ResponsiveContainer width="100%" height="100%">
                        <BarChart data={isOperator ? operatorBedTypeData : hospitalData}>
                          <CartesianGrid
                            strokeDasharray="3 3"
                            vertical={false}
                          />
                          <XAxis dataKey="name" />
                          <YAxis allowDecimals={false} />
                          <Tooltip />
                          <Bar
                            dataKey="available"
                            name="Available"
                            fill="#159a83"
                          />
                          <Bar
                            dataKey="occupied"
                            name="Occupied"
                            fill="#f0a44b"
                          />
                        </BarChart>
                      </ResponsiveContainer>
                    ) : (
                      <div className="empty-state">
                        {isOperator
                          ? `No bed capacity data recorded for ${hospitalName}.`
                          : "No hospital resource data available across network."}
                      </div>
                    )}
                  </div>
                </article>

                <article className="panel">
                  <div className="panel-heading">
                    <div>
                      <h3>Bed occupancy</h3>
                      <p>{isOperator ? `Bed availability in ${hospitalName}` : "Network-wide availability"}</p>
                    </div>
                  </div>

                  <div className="donut-wrap">
                    <ResponsiveContainer width="100%" height={210}>
                      <PieChart>
                        <Pie
                          data={bedData}
                          dataKey="value"
                          nameKey="name"
                          innerRadius={62}
                          outerRadius={83}
                          paddingAngle={4}
                        >
                          {bedData.map((item) => (
                            <Cell
                              key={item.name}
                              fill={item.color}
                            />
                          ))}
                        </Pie>
                        <Tooltip />
                      </PieChart>
                    </ResponsiveContainer>

                    <div className="donut-center">
                      <strong>{summary?.totalBeds ?? 0}</strong>
                      <span>Total beds</span>
                    </div>
                  </div>

                  <div className="legend">
                    {bedData.map((item) => (
                      <div key={item.name}>
                        <span
                          className="legend-dot"
                          style={{ background: item.color }}
                        />
                        <span>{item.name}</span>
                        <strong>{item.value}</strong>
                      </div>
                    ))}
                  </div>
                </article>
              </div>

              <article className="panel patient-panel">
                <div className="panel-heading patient-heading">
                  <div>
                    <h3>Recent patients</h3>
                    <p>Latest patient records from the hospital network</p>
                  </div>

                  <button
                    className="text-button"
                    onClick={() => setActivePage("Patients")}
                  >
                    Manage patients <ChevronRight size={16} />
                  </button>
                </div>

                <PatientTable patients={patients.slice(0, 5)} hospitalName={hospitalName} isOperator={isOperator} />
              </article>
            </>
          )}

          {/* PATIENT MANAGEMENT */}
          {activePage === "Patients" && (
            <PatientManagement
              patients={patients}
              onRefresh={loadData}
              currentUser={currentUser}
            />
          )}

          {/* HOSPITALS & NETWORK (ADMIN ONLY) */}
          {activePage === "Hospitals & Network" && (
            <HospitalsManagement />
          )}

          {/* BED MANAGEMENT */}
          {activePage === "Bed Management" && (
            <>
              <div className="stats-grid">
                <StatCard
                  title="Total beds"
                  value={summary?.totalBeds}
                  subtitle="Across all hospitals"
                  icon={BedDouble}
                  color="blue"
                />

                <StatCard
                  title="Available beds"
                  value={summary?.availableBeds}
                  subtitle="Ready for allocation"
                  icon={BedDouble}
                  color="green"
                />

                <StatCard
                  title="Occupied beds"
                  value={summary?.occupiedBeds}
                  subtitle="Currently assigned"
                  icon={HeartPulse}
                  color="orange"
                />

                <StatCard
                  title="ICU beds"
                  value={summary?.totalIcuBeds}
                  subtitle={`${summary?.availableIcuBeds ?? 0} available`}
                  icon={Activity}
                  color="purple"
                />
              </div>

              <BedManagement currentUser={currentUser} />
            </>
          )}

          {/* DOCTOR MANAGEMENT */}
          {activePage === "Doctors" && (
            <DoctorManagement currentUser={currentUser} />
          )}

          {/* REFERRALS — PRESERVED DIJKSTRA WORKFLOW */}
          {activePage === "Referrals" && (
            <ReferralManagement currentUser={currentUser} />
          )}

          {/* USER MANAGEMENT (ADMIN ONLY) */}
          {activePage === "User Accounts" && (
            <UserManagement />
          )}

          <footer className="footer">
            <span>CareGrid · Government Hospital Resource Allocation System</span>
            <span>
              Logged in as {currentUser.fullName || currentUser.username} ({currentUser.role})
            </span>
          </footer>
        </section>
      </main>
    </div>
  );
}

function PatientTable({ patients, hospitalName, isOperator }) {
  if (!patients?.length) {
    return (
      <div className="empty-state">
        {isOperator
          ? `No patients currently registered or admitted at ${hospitalName || "your hospital"}.`
          : "No patients found in the network."}
      </div>
    );
  }

  return (
    <div className="table-scroll">
      <table className="patient-table">
        <thead>
          <tr>
            <th>Patient</th>
            <th>Severity</th>
            <th>Status</th>
            <th>Bed ID</th>
          </tr>
        </thead>

        <tbody>
          {patients.map((patient) => (
            <tr key={patient.id}>
              <td>
                <div className="patient-cell">
                  <div className="patient-avatar">
                    {(patient.patientName || "P")[0].toUpperCase()}
                  </div>

                  <div>
                    <strong>{patient.patientName}</strong>
                    <span>Patient #{patient.id}</span>
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
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
