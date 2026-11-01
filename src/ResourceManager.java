public class ResourceManager {

    private Hospital hospital;

    public ResourceManager(Hospital hospital) {
        this.hospital = hospital;
    }

    // ------------------------------------------------
    // Find an available bed of required type
    // ------------------------------------------------

    public Bed findAvailableBed(String requiredFacility) {

        for (Bed bed : hospital.getBeds()) {

            if (bed.getType().equalsIgnoreCase(requiredFacility)
                    && bed.isAvailable()) {

                return bed;
            }
        }

        return null;
    }

    // ------------------------------------------------
    // Find an available doctor of required specialization
    // ------------------------------------------------

    public Doctor findAvailableDoctor(String requiredFacility) {

        for (Doctor doctor : hospital.getDoctors()) {

            if (doctor.getSpecialization().equalsIgnoreCase(requiredFacility)
                    && doctor.isAvailable()) {

                return doctor;
            }
        }

        return null;
    }

    // ------------------------------------------------
    // Check bed availability
    // ------------------------------------------------

    public boolean isBedAvailable(String requiredFacility) {

        return findAvailableBed(requiredFacility) != null;
    }

    // ------------------------------------------------
    // Check doctor availability
    // ------------------------------------------------

    public boolean isDoctorAvailable(String requiredFacility) {

        return findAvailableDoctor(requiredFacility) != null;
    }

    // ------------------------------------------------
    // Allocate bed only
    // ------------------------------------------------

    public boolean allocateBed(Patient patient) {

        Bed bed = findAvailableBed(patient.getRequiredFacility());

        if (bed == null) {
            return false;
        }

        bed.setAvailable(false);

        patient.setAssignedHospital(
                hospital.getHospitalName()
        );

        patient.setAssignedBed(
                bed.getBedId()
        );

        return true;
    }

    // ------------------------------------------------
    // Admit patient
    // ------------------------------------------------

    public boolean admitPatient(Patient patient) {

        Bed bed =
                findAvailableBed(
                        patient.getRequiredFacility()
                );

        Doctor doctor =
                findAvailableDoctor(
                        patient.getRequiredFacility()
                );

        // Both resources required
        if (bed == null || doctor == null) {
            return false;
        }

        // Occupy bed
        bed.setAvailable(false);

        // Store patient tracking information
        patient.setAssignedHospital(
                hospital.getHospitalName()
        );

        patient.setAssignedBed(
                bed.getBedId()
        );

        patient.setAssignedDoctor(
                doctor.getName()
        );

        patient.setStatus("ADMITTED");

        System.out.println();
        System.out.println("========== PATIENT ADMITTED ==========");
        System.out.println(
                "Patient ID     : " +
                patient.getPatientId()
        );

        System.out.println(
                "Patient Name   : " +
                patient.getName()
        );

        System.out.println(
                "Hospital       : " +
                hospital.getHospitalName()
        );

        System.out.println(
                "Bed            : " +
                bed.getBedId()
        );

        System.out.println(
                "Assigned Doctor: " +
                doctor.getName()
        );

        System.out.println(
                "Doctor Status  : AVAILABLE"
        );

        System.out.println("======================================");

        return true;
    }

    // ------------------------------------------------
    // Accommodate while waiting for specialist
    // ------------------------------------------------

    public boolean accommodateWhileWaitingForDoctor(
            Patient patient) {

        Bed bed =
                findAvailableBed(
                        patient.getRequiredFacility()
                );

        if (bed == null) {
            return false;
        }

        bed.setAvailable(false);

        patient.setAssignedHospital(
                hospital.getHospitalName()
        );

        patient.setAssignedBed(
                bed.getBedId()
        );

        patient.setAssignedDoctor(
                "WAITING FOR SPECIALIST"
        );

        patient.setStatus(
                "WAITING_FOR_SPECIALIST"
        );

        System.out.println();
        System.out.println(
                "===== PATIENT ACCOMMODATED ====="
        );

        System.out.println(
                "Patient ID : " +
                patient.getPatientId()
        );

        System.out.println(
                "Hospital   : " +
                hospital.getHospitalName()
        );

        System.out.println(
                "Bed        : " +
                bed.getBedId()
        );

        System.out.println(
                "Doctor     : WAITING FOR SPECIALIST"
        );

        System.out.println(
                "Status     : WAITING_FOR_SPECIALIST"
        );

        System.out.println(
                "================================"
        );

        return true;
    }

    // ------------------------------------------------
    // Start doctor consultation
    // ------------------------------------------------

    public boolean startDoctorConsultation(
            Patient patient) {

        Doctor doctor =
                findAvailableDoctor(
                        patient.getRequiredFacility()
                );

        if (doctor == null) {

            System.out.println();
            System.out.println(
                    "No suitable doctor is currently available."
            );

            return false;
        }

        // Doctor is busy only during consultation
        doctor.setAvailable(false);

        patient.setAssignedDoctor(
                doctor.getName()
        );

        patient.setStatus(
                "UNDER_CONSULTATION"
        );

        System.out.println();
        System.out.println(
                "====== CONSULTATION STARTED ======"
        );

        System.out.println(
                "Patient ID    : " +
                patient.getPatientId()
        );

        System.out.println(
                "Patient Name  : " +
                patient.getName()
        );

        System.out.println(
                "Hospital      : " +
                hospital.getHospitalName()
        );

        System.out.println(
                "Doctor        : " +
                doctor.getName()
        );

        System.out.println(
                "Doctor Status : ATTENDING"
        );

        System.out.println(
                "Status        : UNDER_CONSULTATION"
        );

        System.out.println(
                "=================================="
        );

        return true;
    }

    // ------------------------------------------------
    // Finish doctor consultation
    // ------------------------------------------------

    public boolean finishDoctorConsultation(
            String doctorId) {

        for (Doctor doctor :
                hospital.getDoctors()) {

            if (doctor.getDoctorId()
                    .equalsIgnoreCase(doctorId)) {

                doctor.setAvailable(true);

                System.out.println();
                System.out.println(
                        "====== CONSULTATION COMPLETED ======"
                );

                System.out.println(
                        "Doctor        : " +
                        doctor.getName()
                );

                System.out.println(
                        "Doctor Status : AVAILABLE"
                );

                System.out.println(
                        "===================================="
                );

                return true;
            }
        }

        System.out.println(
                "Doctor not found."
        );

        return false;
    }

    // ------------------------------------------------
    // DISCHARGE PATIENT
    // ------------------------------------------------

    public boolean dischargePatient(
            Patient patient) {

        String bedId =
                patient.getAssignedBed();

        if (bedId == null
                || bedId.equals("NOT ASSIGNED")) {

            System.out.println();
            System.out.println(
                    "No bed is assigned to this patient."
            );

            return false;
        }

        // Find the patient's bed
        for (Bed bed :
                hospital.getBeds()) {

            if (bed.getBedId()
                    .equalsIgnoreCase(bedId)) {

                // Release the bed
                bed.setAvailable(true);

                // Update patient status
                patient.setStatus(
                        "DISCHARGED"
                );

                System.out.println();
                System.out.println(
                        "========== PATIENT DISCHARGED =========="
                );

                System.out.println(
                        "Patient ID : " +
                        patient.getPatientId()
                );

                System.out.println(
                        "Patient    : " +
                        patient.getName()
                );

                System.out.println(
                        "Hospital   : " +
                        patient.getAssignedHospital()
                );

                System.out.println(
                        "Bed        : " +
                        bed.getBedId()
                );

                System.out.println(
                        "Status     : DISCHARGED"
                );

                System.out.println();
                System.out.println(
                        "Bed " +
                        bed.getBedId() +
                        " is now AVAILABLE."
                );

                System.out.println(
                        "========================================="
                );

                return true;
            }
        }

        System.out.println();
        System.out.println(
                "Assigned bed could not be found."
        );

        return false;
    }

    // ------------------------------------------------
    // Get hospital
    // ------------------------------------------------

    public Hospital getHospital() {

        return hospital;
    }
}