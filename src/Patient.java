public class Patient {

    private String patientId;
    private String name;
    private String severity;
    private String requiredFacility;
    private int arrivalOrder;
    private String status;

    // Patient tracking details
    private String assignedHospital;
    private String assignedBed;
    private String assignedDoctor;

    public Patient(String patientId, String name, String severity,
                   String requiredFacility, int arrivalOrder) {

        this.patientId = patientId;
        this.name = name;
        this.severity = severity;
        this.requiredFacility = requiredFacility;
        this.arrivalOrder = arrivalOrder;

        this.status = "WAITING";

        this.assignedHospital = "NOT ASSIGNED";
        this.assignedBed = "NOT ASSIGNED";
        this.assignedDoctor = "NOT ASSIGNED";
    }

    // -------------------------
    // Basic Patient Details
    // -------------------------

    public String getPatientId() {
        return patientId;
    }

    public String getName() {
        return name;
    }

    public String getSeverity() {
        return severity;
    }

    public String getRequiredFacility() {
        return requiredFacility;
    }

    public int getArrivalOrder() {
        return arrivalOrder;
    }

    // -------------------------
    // Status
    // -------------------------

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // -------------------------
    // Hospital Tracking
    // -------------------------

    public String getAssignedHospital() {
        return assignedHospital;
    }

    public void setAssignedHospital(String assignedHospital) {
        this.assignedHospital = assignedHospital;
    }

    // -------------------------
    // Bed Tracking
    // -------------------------

    public String getAssignedBed() {
        return assignedBed;
    }

    public void setAssignedBed(String assignedBed) {
        this.assignedBed = assignedBed;
    }

    // -------------------------
    // Doctor Tracking
    // -------------------------

    public String getAssignedDoctor() {
        return assignedDoctor;
    }

    public void setAssignedDoctor(String assignedDoctor) {
        this.assignedDoctor = assignedDoctor;
    }

    // -------------------------
    // Display
    // -------------------------

    @Override
    public String toString() {

        return "Patient ID      : " + patientId +
               "\nName             : " + name +
               "\nSeverity         : " + severity +
               "\nRequired Facility: " + requiredFacility +
               "\nArrival Order    : " + arrivalOrder +
               "\nStatus           : " + status +
               "\nHospital         : " + assignedHospital +
               "\nBed              : " + assignedBed +
               "\nAssigned Doctor  : " + assignedDoctor;
    }
}