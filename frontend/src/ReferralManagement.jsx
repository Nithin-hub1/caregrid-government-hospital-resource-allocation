import { authFetch } from "./api";
import { useEffect, useState } from "react";
import {
  Building2,
  Clock3,
  BedDouble,
  Stethoscope,
  MapPin,
  RefreshCw,
  Search,
  Route,
  ShieldCheck,
  AlertCircle,
  UserRound,
  CheckCircle2,
  Network,
  Truck,
  ArrowRight,
} from "lucide-react";
import "./App.css";

const API = "/api";

export default function ReferralManagement({ currentUser }) {
  const [patients, setPatients] = useState([]);
  const [hospitals, setHospitals] = useState([]);
  const [networkData, setNetworkData] = useState(null);
  const [patientId, setPatientId] = useState("");
  const [sourceHospitalId, setSourceHospitalId] = useState("");
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [successNotice, setSuccessNotice] = useState("");
  const [loading, setLoading] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [transferring, setTransferring] = useState(false);
  const [loadingData, setLoadingData] = useState(true);

  async function loadOptions() {
    setLoadingData(true);
    setError("");

    try {
      const [patientResponse, hospitalResponse, networkResponse] = await Promise.all([
        authFetch(`${API}/patients`),
        authFetch(`${API}/hospitals`),
        authFetch(`${API}/network-locations/overview`).catch(() => null),
      ]);

      if (!patientResponse.ok || !hospitalResponse.ok) {
        throw new Error("Unable to load patients or hospitals.");
      }

      const patientData = await patientResponse.json();
      const hospitalData = await hospitalResponse.json();
      const netData = networkResponse && networkResponse.ok ? await networkResponse.json() : null;

      const patientList = Array.isArray(patientData) ? patientData : [];
      const hospitalList = Array.isArray(hospitalData) ? hospitalData : [];

      setPatients(patientList);
      setHospitals(hospitalList);
      setNetworkData(netData);

      if (currentUser?.role === "HOSPITAL_OPERATOR" && currentUser?.hospitalId) {
        setSourceHospitalId(String(currentUser.hospitalId));
      } else if (!sourceHospitalId && hospitalList.length) {
        const primary = hospitalList.find((h) => h.primaryHospital === true);
        const firstActive = primary ?? hospitalList.find((h) => h.active !== false) ?? hospitalList[0];
        setSourceHospitalId(String(firstActive.id));
      }
    } catch (err) {
      setError(err.message || "Unable to load referral data.");
    } finally {
      setLoadingData(false);
    }
  }

  useEffect(() => {
    loadOptions();
  }, []);

  async function findBestHospital(event) {
    event.preventDefault();
    setError("");
    setSuccessNotice("");
    setResult(null);

    if (!patientId || !sourceHospitalId) {
      setError("Select a patient and source hospital.");
      return;
    }

    const patient = patients.find(
      (item) => String(item.id) === String(patientId)
    );

    if (!patient) {
      setError("The selected patient could not be found.");
      return;
    }

    if (patient.status === "DISCHARGED") {
      setError("This patient has already been discharged.");
      return;
    }

    setLoading(true);

    try {
      const response = await authFetch(
        `${API}/referrals/patients/${encodeURIComponent(patientId)}?sourceHospitalId=${encodeURIComponent(sourceHospitalId)}`,
        { method: "POST" }
      );

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        throw new Error(
          data.error || data.message || "Referral request failed."
        );
      }

      setResult(data);
    } catch (err) {
      setError(err.message || "Could not calculate the referral.");
    } finally {
      setLoading(false);
    }
  }

  async function confirmReferralAction() {
    if (!result || !patientId || !sourceHospitalId) return;

    const recommendedId = result.recommendedHospitalId;
    const bedId = result.availableBedId;
    const doctorId = result.availableDoctorId;

    if (!recommendedId || !bedId || !doctorId) {
      setError("Incomplete recommendation data to confirm referral.");
      return;
    }

    setConfirming(true);
    setError("");
    setSuccessNotice("");

    try {
      const response = await authFetch(`${API}/referrals/confirm`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          patientId: Number(patientId),
          sourceHospitalId: Number(sourceHospitalId),
          destinationHospitalId: Number(recommendedId),
          bedId: Number(bedId),
          doctorId: Number(doctorId),
        }),
      });

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        throw new Error(data.message || data.error || "Confirmation failed.");
      }

      setResult(data);
      setSuccessNotice(
        `Referral confirmed! Bed #${data.availableBedNumber || bedId} and doctor ${
          data.availableDoctorName || "assigned"
        } have been reserved at ${data.recommendedHospitalName}. Patient status updated to REFERRED.`
      );

      await loadOptions();
    } catch (err) {
      setError(err.message || "Failed to confirm referral and reserve resources.");
    } finally {
      setConfirming(false);
    }
  }

  async function completeTransferAction(id) {
    const targetId = id || patientId;
    if (!targetId) return;

    setTransferring(true);
    setError("");
    setSuccessNotice("");

    try {
      const response = await authFetch(
        `${API}/referrals/patients/${encodeURIComponent(targetId)}/complete-transfer`,
        { method: "POST" }
      );

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        throw new Error(data.message || data.error || "Transfer completion failed.");
      }

      setSuccessNotice(
        data.message ||
          `Transfer completed! Patient #${targetId} is now admitted at the destination hospital.`
      );
      setResult(null);

      await loadOptions();
    } catch (err) {
      setError(err.message || "Could not complete patient transfer.");
    } finally {
      setTransferring(false);
    }
  }

  const selectedPatient = patients.find(
    (patient) => String(patient.id) === String(patientId)
  );

  const selectedHospital = hospitals.find(
    (hospital) => String(hospital.id) === String(sourceHospitalId)
  );

  const recommendedId =
    result?.recommendedHospitalId ??
    result?.destinationHospitalId ??
    result?.hospitalId;

  const recommendedName =
    result?.recommendedHospitalName ??
    result?.destinationHospitalName ??
    result?.hospitalName ??
    hospitals.find((hospital) => String(hospital.id) === String(recommendedId))
      ?.name;

  const bedId = result?.availableBedId ?? result?.bedId;
  const bedNumber = result?.availableBedNumber ?? result?.bedNumber;
  const bedType = result?.requiredBedType ?? result?.bedType;
  const doctorName = result?.availableDoctorName ?? result?.doctorName;
  const doctorId = result?.availableDoctorId ?? result?.doctorId;
  const travelMinutes = result?.estimatedTravelMinutes ?? result?.travelMinutes;
  const route = result?.route;

  const routeText = Array.isArray(route)
    ? route
        .map((stop) =>
          typeof stop === "object"
            ? stop.toHospitalName ?? stop.hospitalName ?? stop.name ?? stop.id
            : stop
        )
        .join(" → ")
    : typeof route === "string"
      ? route
      : null;

  const hasRecommendation =
    recommendedName != null ||
    recommendedId != null ||
    bedId != null ||
    bedNumber != null;

  const isAlreadyReferred = selectedPatient?.status === "REFERRED";

  return (
    <section className="content-section referral-management">
      <div className="section-heading">
        <div>
          <p className="eyebrow">SMART RESOURCE COORDINATION</p>
          <h2>Hospital Referrals</h2>
          <p className="muted">
            Find the fastest reachable referral hospital using Dijkstra algorithm over
            the connected network, and safely reserve bed and doctor resources before confirming.
          </p>
        </div>

        <button
          className="refresh-button"
          type="button"
          onClick={loadOptions}
          disabled={loadingData || loading}
        >
          <RefreshCw size={16} />
          Refresh data
        </button>
      </div>

      {successNotice && (
        <div className="referral-success-badge" role="status">
          <CheckCircle2 size={18} />
          <span>{successNotice}</span>
        </div>
      )}

      {error && (
        <div className="referral-error">
          <AlertCircle size={20} />
          <div>
            <strong>Referral Alert</strong>
            <p>{error}</p>
          </div>
        </div>
      )}

      <div className="referral-form-card">
        <div className="referral-form-heading">
          <div className="referral-icon">
            <Route size={24} />
          </div>
          <div>
            <h3>Find the best hospital</h3>
            <p>Select the patient and their current or primary hospital.</p>
          </div>
        </div>

        <form className="referral-form" onSubmit={findBestHospital}>
          <div className="referral-field">
            <label htmlFor="referral-patient">Select patient</label>
            <select
              id="referral-patient"
              value={patientId}
              onChange={(event) => {
                setPatientId(event.target.value);
                setResult(null);
                setError("");
                setSuccessNotice("");
              }}
              required
              disabled={loadingData || loading}
            >
              <option value="">Choose a patient</option>
              {patients.map((patient) => (
                <option key={patient.id} value={patient.id}>
                  #{patient.id} — {patient.patientName} ({patient.severity}) · [{patient.status}]
                </option>
              ))}
            </select>
          </div>

          <div className="referral-field">
            <label htmlFor="referral-source">Current hospital</label>
            <select
              id="referral-source"
              value={sourceHospitalId}
              onChange={(event) => {
                setSourceHospitalId(event.target.value);
                setResult(null);
                setError("");
                setSuccessNotice("");
              }}
              required
              disabled={loadingData || loading}
            >
              <option value="">Choose source hospital</option>
              {hospitals
                .filter((hospital) => hospital.active !== false)
                .map((hospital) => (
                  <option key={hospital.id} value={hospital.id}>
                    {hospital.name} — #{hospital.id} {hospital.primaryHospital ? "(Primary)" : "(Referral)"}
                  </option>
                ))}
            </select>
          </div>

          {selectedPatient && (
            <div className="selected-patient-summary">
              <UserRound size={19} />
              <div>
                <strong>{selectedPatient.patientName}</strong>
                <span>
                  Patient #{selectedPatient.id} · Severity: {selectedPatient.severity} · Status:{" "}
                  <strong>{selectedPatient.status}</strong>
                </span>
              </div>
            </div>
          )}

          {selectedHospital && (
            <div className="selected-source-summary">
              <Building2 size={18} />
              <span>
                Referring from: <strong>{selectedHospital.name}</strong>{" "}
                {selectedHospital.primaryHospital ? "(Designated Primary Hospital)" : "(Referral Hospital)"}
              </span>
            </div>
          )}

          {isAlreadyReferred && (
            <div style={{ padding: "12px 14px", background: "#ebf8ff", borderRadius: 8, color: "#2b6cb0", display: "flex", alignItems: "center", justifyContent: "space-between", gap: 10 }}>
              <span>
                This patient is already in <strong>REFERRED</strong> status with resources reserved.
              </span>
              <button
                type="button"
                className="transfer-button"
                onClick={() => completeTransferAction(selectedPatient.id)}
                disabled={transferring}
              >
                <Truck size={15} />
                {transferring ? "Completing..." : "Complete Transfer"}
              </button>
            </div>
          )}

          <button
            type="submit"
            className="primary-button referral-submit"
            disabled={
              loading || loadingData || !patientId || !sourceHospitalId || isAlreadyReferred
            }
          >
            {loading ? (
              <>
                <RefreshCw size={17} className="spin" />
                Calculating fastest reachable hospital...
              </>
            ) : (
              <>
                <Search size={17} />
                Find Best Hospital
              </>
            )}
          </button>
        </form>
      </div>

      {loadingData && (
        <div className="referral-message">
          <RefreshCw size={18} className="spin" />
          Loading patients, hospitals, and connected network...
        </div>
      )}

      {result && hasRecommendation && (
        <div className="referral-result">
          <div className="referral-result-header">
            <div className="referral-result-icon">
              <ShieldCheck size={25} />
            </div>
            <div>
              <p className="eyebrow">DIJKSTRA NETWORK RECOMMENDATION</p>
              <h3>{recommendedName || `Hospital #${recommendedId}`}</h3>
              <p>{result.message || "Fastest eligible destination with matching bed and doctor."}</p>
            </div>
          </div>

          <div className="referral-result-grid">
            <div className="referral-result-item">
              <div className="result-item-icon bed-result-icon">
                <BedDouble size={21} />
              </div>
              <span>Compatible bed</span>
              <strong>
                {bedNumber || (bedId != null ? `Bed #${bedId}` : "Not specified")}
              </strong>
              {bedType && <small>Facility: {bedType}</small>}
              {bedId != null && <small>Bed ID: {bedId}</small>}
            </div>

            <div className="referral-result-item">
              <div className="result-item-icon doctor-result-icon">
                <Stethoscope size={21} />
              </div>
              <span>Available doctor</span>
              <strong>{doctorName || "Not specified"}</strong>
              {doctorId != null && <small>Doctor ID: {doctorId}</small>}
            </div>

            <div className="referral-result-item">
              <div className="result-item-icon travel-result-icon">
                <Clock3 size={21} />
              </div>
              <span>Fastest travel time</span>
              <strong>
                {travelMinutes != null
                  ? `${travelMinutes} minutes`
                  : "Not specified"}
              </strong>
              <small>Calculated via shortest path</small>
            </div>
          </div>

          {(routeText || result.sourceHospitalName) && (
            <div className="referral-route">
              <MapPin size={19} />
              <div>
                <strong>Network Route Navigation</strong>
                <p>
                  {result.sourceHospitalName} → {routeText ? routeText : recommendedName}
                </p>
              </div>
            </div>
          )}

          {result.status === "REFERRED" ? (
            <div style={{ marginTop: 18, display: "flex", alignItems: "center", justifyContent: "space-between", flexWrap: "wrap", gap: 12 }}>
              <div className="referral-success-badge" style={{ margin: 0 }}>
                <CheckCircle2 size={17} />
                <span>Resources safely locked & reserved. Patient is in transit.</span>
              </div>
              <button
                type="button"
                className="confirm-referral-button"
                style={{ margin: 0, background: "#2b6cb0" }}
                onClick={() => completeTransferAction(patientId)}
                disabled={transferring}
              >
                <Truck size={16} />
                {transferring ? "Completing..." : "Complete Transfer (Confirm Arrival)"}
              </button>
            </div>
          ) : (
            <button
              type="button"
              className="confirm-referral-button"
              onClick={confirmReferralAction}
              disabled={confirming}
            >
              <CheckCircle2 size={17} />
              {confirming ? "Reserving resources..." : "Confirm Referral & Reserve Resources"}
            </button>
          )}
        </div>
      )}

      {result && !hasRecommendation && (
        <div className="referral-error">
          <AlertCircle size={20} />
          <div>
            <strong>No suitable referral destination available</strong>
            <p>
              No reachable hospital currently has both an available compatible bed and an available doctor.
              The patient should be kept in the priority waiting queue.
            </p>
          </div>
        </div>
      )}

      {/* CONNECTED NETWORK VIEWER (8–10 LOCATIONS) */}
      <section className="network-card">
        <div className="network-card-header">
          <div>
            <p className="eyebrow">REGIONAL HEALTH INFRASTRUCTURE</p>
            <h3 style={{ margin: "4px 0 0", fontSize: 18, fontWeight: 750 }}>
              Connected Network (10 Locations, 1 Primary & 3 Referral Hospitals)
            </h3>
            <p style={{ margin: "4px 0 0", fontSize: 13, color: "#718096" }}>
              Topology of primary and referral healthcare facilities linked with key transit junctions.
            </p>
          </div>

          <span className="live-tag">
            <Network size={14} style={{ display: "inline", verticalAlign: "middle", marginRight: 4 }} />
            {networkData?.locations?.length || 10} NODES · {networkData?.routes?.length || 36} EDGES
          </span>
        </div>

        <div className="network-locations-grid">
          {networkData?.locations?.map((loc) => {
            const hosp = hospitals.find(
              (h) => h.networkLocation?.id === loc.id || (h.id === loc.id && loc.id <= 4)
            );
            const isPrimary = hosp?.primaryHospital;
            const isReferral = hosp && !hosp.primaryHospital;
            const cardClass = isPrimary ? "primary" : isReferral ? "referral" : "transit";
            const badgeClass = isPrimary ? "primary" : isReferral ? "referral" : "transit";
            const badgeText = isPrimary
              ? "Primary Hospital"
              : isReferral
                ? "Referral Hospital"
                : "Transit Checkpoint";

            return (
              <div key={loc.id} className={`network-node-card ${cardClass}`}>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                  <span className={`network-node-badge ${badgeClass}`}>{badgeText}</span>
                  <small style={{ color: "#a0aec0", fontSize: 11 }}>Node #{loc.id}</small>
                </div>
                <strong style={{ fontSize: 13, color: "#1a202c" }}>{loc.name}</strong>
                <span style={{ fontSize: 11, color: "#718096" }}>
                  {hosp ? hosp.name : `Junction · ${loc.city}`}
                </span>
              </div>
            );
          })}
        </div>

        <div style={{ marginTop: 10 }}>
          <h4 style={{ margin: "0 0 10px", fontSize: 13, color: "#4a5568", fontWeight: 700 }}>
            Active Route Connections & Travel Times
          </h4>
          <div className="network-routes-list">
            {networkData?.routes?.slice(0, 18).map((route) => (
              <div key={route.id} className="network-route-item">
                <span>
                  {route.sourceLocationName} <ArrowRight size={11} style={{ display: "inline", margin: "0 4px" }} /> {route.destinationLocationName}
                </span>
                <span className="network-route-time">{route.travelMinutes} mins</span>
              </div>
            ))}
          </div>
        </div>
      </section>
    </section>
  );
}
