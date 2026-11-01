import java.util.Scanner;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

public class HospitalSystem {

    // =========================================================
    // VARIABLES
    // =========================================================

    private Hospital primaryHospital;

    private Hospital[] referralHospitals;

    private HospitalNetwork network;

    private PatientPriorityQueue patientQueue;

    private ResourceManager resourceManager;

    private ReferralManager referralManager;

    private Scanner scanner;

    /*
     * Stores every patient using Patient ID.
     * This allows patient verification and tracking.
     */
    private Map<String, Patient> patientRecords;

    /*
     * Used to maintain arrival order.
     * Smaller number = earlier arrival.
     */
    private int arrivalCounter = 0;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public HospitalSystem() {

        scanner = new Scanner(System.in);

        patientQueue = new PatientPriorityQueue();

        patientRecords = new HashMap<>();

        setupHospitals();

        setupNetwork();

        resourceManager =
                new ResourceManager(primaryHospital);

        referralManager =
                new ReferralManager(network);
    }


    // =========================================================
    // CREATE HOSPITALS
    // =========================================================

    private void setupHospitals() {

        // =====================================================
        // PRIMARY HOSPITAL - HOSPITAL A
        // =====================================================

        primaryHospital =
                new Hospital(
                        "H001",
                        "Hospital A"
                );


        // -----------------------------------------------------
        // ICU BEDS
        // -----------------------------------------------------

        primaryHospital.addBed(
                new Bed(
                        "A-ICU-01",
                        "ICU",
                        true
                )
        );

        primaryHospital.addBed(
                new Bed(
                        "A-ICU-02",
                        "ICU",
                        true
                )
        );


        // -----------------------------------------------------
        // GENERAL BEDS
        // -----------------------------------------------------

        primaryHospital.addBed(
                new Bed(
                        "A-GEN-01",
                        "GENERAL",
                        true
                )
        );

        primaryHospital.addBed(
                new Bed(
                        "A-GEN-02",
                        "GENERAL",
                        true
                )
        );


        // -----------------------------------------------------
        // DOCTORS
        // -----------------------------------------------------

        primaryHospital.addDoctor(
                new Doctor(
                        "A-D001",
                        "Dr. Kumar",
                        "ICU",
                        true
                )
        );

        primaryHospital.addDoctor(
                new Doctor(
                        "A-D002",
                        "Dr. Priya",
                        "GENERAL",
                        true
                )
        );


        // =====================================================
        // REFERRAL HOSPITAL B
        // =====================================================

        Hospital hospitalB =
                new Hospital(
                        "H002",
                        "Hospital B"
                );


        /*
         * ICU bed initially unavailable.
         * This helps demonstrate referral logic.
         */
        hospitalB.addBed(
                new Bed(
                        "B-ICU-01",
                        "ICU",
                        false
                )
        );

        hospitalB.addDoctor(
                new Doctor(
                        "B-D001",
                        "Dr. Arun",
                        "ICU",
                        true
                )
        );


        // =====================================================
        // REFERRAL HOSPITAL C
        // =====================================================

        Hospital hospitalC =
                new Hospital(
                        "H003",
                        "Hospital C"
                );

        hospitalC.addBed(
                new Bed(
                        "C-ICU-01",
                        "ICU",
                        true
                )
        );

        hospitalC.addDoctor(
                new Doctor(
                        "C-D001",
                        "Dr. Meena",
                        "ICU",
                        true
                )
        );


        // =====================================================
        // REFERRAL HOSPITAL D
        // =====================================================

        Hospital hospitalD =
                new Hospital(
                        "H004",
                        "Hospital D"
                );

        hospitalD.addBed(
                new Bed(
                        "D-ICU-01",
                        "ICU",
                        true
                )
        );

        hospitalD.addDoctor(
                new Doctor(
                        "D-D001",
                        "Dr. Raj",
                        "ICU",
                        true
                )
        );


        // =====================================================
        // STORE REFERRAL HOSPITALS
        // =====================================================

        referralHospitals =
                new Hospital[]{
                        hospitalB,
                        hospitalC,
                        hospitalD
                };
    }


    // =========================================================
    // CREATE HOSPITAL NETWORK
    // =========================================================

    private void setupNetwork() {

        network =
                new HospitalNetwork();


        // =====================================================
        // HOSPITAL A CONNECTIONS
        // =====================================================

        network.addConnection(
                "Hospital A",
                "Hospital B",
                10
        );

        network.addConnection(
                "Hospital A",
                "Location 1",
                5
        );

        network.addConnection(
                "Hospital A",
                "Location 2",
                12
        );


        // =====================================================
        // HOSPITAL B CONNECTIONS
        // =====================================================

        network.addConnection(
                "Hospital B",
                "Location 1",
                8
        );

        network.addConnection(
                "Hospital B",
                "Location 3",
                6
        );


        // =====================================================
        // HOSPITAL C CONNECTIONS
        // =====================================================

        network.addConnection(
                "Hospital C",
                "Location 2",
                7
        );

        network.addConnection(
                "Hospital C",
                "Location 3",
                9
        );


        // =====================================================
        // HOSPITAL D CONNECTIONS
        // =====================================================

        network.addConnection(
                "Hospital D",
                "Location 1",
                10
        );

        network.addConnection(
                "Hospital D",
                "Location 2",
                6
        );


        // =====================================================
        // LOCATION CONNECTIONS
        // =====================================================

        network.addConnection(
                "Location 1",
                "Location 3",
                4
        );

        network.addConnection(
                "Location 2",
                "Location 3",
                5
        );

        network.addConnection(
                "Location 3",
                "Location 4",
                8
        );
    }


    // =========================================================
    // START APPLICATION
    // =========================================================

    public void start() {

        boolean running = true;

        while (running) {

            displayMenu();

            int choice;

            try {

                choice =
                        Integer.parseInt(
                                scanner.nextLine().trim()
                        );

            } catch (NumberFormatException e) {

                System.out.println(
                        "\nInvalid input. Please enter a number."
                );

                continue;
            }


            switch (choice) {

                case 1:

                    addPatient();

                    break;


                case 2:

                    processNextPatient();

                    break;


                case 3:

                    displayWaitingPatients();

                    break;


                case 4:

                    displayAllHospitalResources();

                    break;


                case 5:

                    verifySearchPatient();

                    break;


                case 6:

                    displayNetwork();

                    break;


                case 7:

                    startDoctorConsultation();

                    break;


                case 8:

                    finishDoctorConsultation();

                    break;


                case 9:

                    dischargePatient();

                    break;


                case 10:

                    displayResourceSummary();

                    break;


                case 11:

                    running = false;

                    System.out.println(
                            "\nSystem closed."
                    );

                    break;


                default:

                    System.out.println(
                            "\nInvalid choice. Please select 1-11."
                    );
            }
        }
    }


    // =========================================================
    // DISPLAY MENU
    // =========================================================

    private void displayMenu() {

        System.out.println(
                "\n=============================================="
        );

        System.out.println(
                "   GOVERNMENT HOSPITAL RESOURCE SYSTEM"
        );

        System.out.println(
                "=============================================="
        );

        System.out.println(
                "1. Add Patient"
        );

        System.out.println(
                "2. Process Next Patient"
        );

        System.out.println(
                "3. View Waiting Patients"
        );

        System.out.println(
                "4. View All Hospital Resources"
        );

        System.out.println(
                "5. Verify / Search Patient by ID"
        );

        System.out.println(
                "6. View Hospital Network"
        );

        System.out.println(
                "7. Start Doctor Consultation"
        );

        System.out.println(
                "8. Finish Doctor Consultation"
        );

        System.out.println(
                "9. Discharge Patient"
        );

        System.out.println(
                "10. Resource Summary"
        );

        System.out.println(
                "11. Exit"
        );

        System.out.println(
                "=============================================="
        );

        System.out.print(
                "Enter your choice: "
        );
    }


    // =========================================================
    // ADD PATIENT
    // =========================================================

    private void addPatient() {

        System.out.println(
                "\n=============================================="
        );

        System.out.println(
                "              ADD PATIENT"
        );

        System.out.println(
                "=============================================="
        );


        // -----------------------------------------------------
        // PATIENT ID
        // -----------------------------------------------------

        String id;

        while (true) {

            System.out.print(
                    "Patient ID (P001 format): "
            );

            id =
                    scanner.nextLine()
                            .trim()
                            .toUpperCase();


            if (!id.matches("P\\d{3}")) {

                System.out.println(
                        "Invalid ID. Use format P001, P002, etc."
                );

                continue;
            }


            if (patientRecords.containsKey(id)) {

                System.out.println(
                        "Patient ID already exists."
                );

                continue;
            }

            break;
        }


        // -----------------------------------------------------
        // PATIENT NAME
        // -----------------------------------------------------

        String name;

        while (true) {

            System.out.print(
                    "Patient Name: "
            );

            name =
                    scanner.nextLine().trim();


            if (name.isEmpty()) {

                System.out.println(
                        "Name cannot be empty."
                );

                continue;
            }


            if (!name.matches("[a-zA-Z ]+")) {

                System.out.println(
                        "Name should contain only letters and spaces."
                );

                continue;
            }

            break;
        }


        // -----------------------------------------------------
        // SEVERITY
        // -----------------------------------------------------

        String severity;

        while (true) {

            System.out.print(
                    "Severity (CRITICAL / SERIOUS / MINOR): "
            );

            severity =
                    scanner.nextLine()
                            .trim()
                            .toUpperCase();


            if (severity.equals("CRITICAL")
                    || severity.equals("SERIOUS")
                    || severity.equals("MINOR")) {

                break;
            }


            System.out.println(
                    "Invalid severity."
            );
        }


        // -----------------------------------------------------
        // AUTOMATIC FACILITY ALLOCATION
        // -----------------------------------------------------

        String requiredFacility;


        if (severity.equals("CRITICAL")) {

            requiredFacility = "ICU";

        } else {

            requiredFacility = "GENERAL";
        }


        System.out.println(
                "Required Facility: "
                        + requiredFacility
        );


        // -----------------------------------------------------
        // ARRIVAL ORDER
        // -----------------------------------------------------

        arrivalCounter++;


        // -----------------------------------------------------
        // CREATE PATIENT
        // -----------------------------------------------------

        Patient patient =
                new Patient(
                        id,
                        name,
                        severity,
                        requiredFacility,
                        arrivalCounter
                );


        // -----------------------------------------------------
        // STORE PATIENT
        // -----------------------------------------------------

        patientRecords.put(
                id,
                patient
        );


        // -----------------------------------------------------
        // ADD TO PRIORITY QUEUE
        // -----------------------------------------------------

        patientQueue.addPatient(patient);


        System.out.println(
                "\nPatient added successfully."
        );

        System.out.println(
                "Patient ID       : " + id
        );

        System.out.println(
                "Severity         : " + severity
        );

        System.out.println(
                "Required Facility: " + requiredFacility
        );

        System.out.println(
                "Arrival Order    : " + arrivalCounter
        );

        System.out.println(
                "Status           : WAITING"
        );
    }


    // =========================================================
    // PROCESS NEXT PATIENT
    // =========================================================

    private void processNextPatient() {

        if (patientQueue.isEmpty()) {

            System.out.println(
                    "\nNo patients are waiting."
            );

            return;
        }


        Patient patient =
                patientQueue.getNextPatient();


        System.out.println(
                "\n=============================================="
        );

        System.out.println(
                "             PROCESSING PATIENT"
        );

        System.out.println(
                "=============================================="
        );


        System.out.println(patient);


        // =====================================================
        // CHECK LOCAL RESOURCES
        // =====================================================

        boolean bedAvailable =
                resourceManager.isBedAvailable(
                        patient.getRequiredFacility()
                );


        boolean doctorAvailable =
                resourceManager.isDoctorAvailable(
                        patient.getRequiredFacility()
                );


        // =====================================================
        // BOTH RESOURCES AVAILABLE
        // =====================================================

        if (bedAvailable && doctorAvailable) {

            boolean admitted =
                    resourceManager.admitPatient(patient);


            if (admitted) {

                System.out.println(
                        "\nPatient admitted to "
                                + primaryHospital.getHospitalName()
                );

                return;
            }
        }


        // =====================================================
        // BED UNAVAILABLE
        // =====================================================

        if (!bedAvailable) {

            System.out.println(
                    "\nRequired bed is not available locally."
            );

            System.out.println(
                    "Searching referral hospitals..."
            );


            referPatient(patient);

            return;
        }


        // =====================================================
        // BED AVAILABLE BUT DOCTOR BUSY
        // =====================================================

        if (bedAvailable && !doctorAvailable) {

            System.out.println(
                    "\nRequired specialist is currently unavailable."
            );


            // -------------------------------------------------
            // TRY REFERRAL
            // -------------------------------------------------

            Hospital referralHospital =
                    referralManager.findReferralHospital(
                            patient,
                            primaryHospital,
                            referralHospitals
                    );


            if (referralHospital != null) {

                completeReferral(
                        patient,
                        referralHospital
                );

                return;
            }


            // -------------------------------------------------
            // CRITICAL PATIENT
            // -------------------------------------------------

            if (patient.getSeverity()
                    .equals("CRITICAL")) {

                System.out.println(
                        "\nCritical patient requires immediate resource allocation."
                );

                System.out.println(
                        "Allocating local ICU bed."
                );


                boolean accommodated =
                        resourceManager
                                .accommodateWhileWaitingForDoctor(
                                        patient
                                );


                if (accommodated) {

                    System.out.println(
                            "\nPatient accommodated locally."
                    );

                    System.out.println(
                            "Status: WAITING_FOR_SPECIALIST"
                    );

                } else {

                    patient.setStatus("WAITING");

                    patientQueue.addPatient(patient);
                }

                return;
            }


            // -------------------------------------------------
            // NON-CRITICAL PATIENT
            // -------------------------------------------------

            patient.setStatus("WAITING");

            patientQueue.addPatient(patient);


            System.out.println(
                    "\nPatient remains in waiting queue."
            );

            return;
        }
    }


    // =========================================================
    // REFER PATIENT
    // =========================================================

    private void referPatient(Patient patient) {

        Hospital referralHospital =
                referralManager.findReferralHospital(
                        patient,
                        primaryHospital,
                        referralHospitals
                );


        if (referralHospital == null) {

            System.out.println(
                    "\nNo suitable referral hospital is currently available."
            );


            patient.setStatus("WAITING");

            patientQueue.addPatient(patient);


            System.out.println(
                    "Patient returned to waiting queue."
            );

            return;
        }


        completeReferral(
                patient,
                referralHospital
        );
    }


    // =========================================================
    // COMPLETE REFERRAL
    // =========================================================

    private void completeReferral(
            Patient patient,
            Hospital referralHospital) {


        // -----------------------------------------------------
        // CREATE RESOURCE MANAGER FOR TARGET HOSPITAL
        // -----------------------------------------------------

        ResourceManager referralResourceManager =
                new ResourceManager(
                        referralHospital
                );


        // -----------------------------------------------------
        // ADMIT AT REFERRAL HOSPITAL
        // -----------------------------------------------------

        boolean admitted =
                referralResourceManager.admitPatient(
                        patient
                );


        if (!admitted) {

            patient.setStatus("WAITING");

            patientQueue.addPatient(patient);


            System.out.println(
                    "\nReferral hospital could not allocate resources."
            );

            System.out.println(
                    "Patient returned to waiting queue."
            );

            return;
        }


        // -----------------------------------------------------
        // SET STATUS
        // -----------------------------------------------------

        patient.setStatus("REFERRED");


        // -----------------------------------------------------
        // GET DIJKSTRA INFORMATION
        // -----------------------------------------------------

        int travelTime =
                referralManager.getLastTravelTime();


        List<String> route =
                referralManager.getLastRoute();


        // =====================================================
        // DISPLAY REFERRAL
        // =====================================================

        System.out.println(
                "\n=============================================="
        );

        System.out.println(
                "          PATIENT REFERRED SUCCESSFULLY"
        );

        System.out.println(
                "=============================================="
        );


        System.out.println(
                "Patient : "
                        + patient.getPatientId()
        );


        System.out.println(
                "From    : "
                        + primaryHospital.getHospitalName()
        );


        System.out.println(
                "To      : "
                        + referralHospital.getHospitalName()
        );


        // -----------------------------------------------------
        // DISPLAY ROUTE
        // -----------------------------------------------------

        System.out.print(
                "Route   : "
        );


        if (route != null && !route.isEmpty()) {

            for (int i = 0;
                 i < route.size();
                 i++) {

                System.out.print(
                        route.get(i)
                );


                if (i < route.size() - 1) {

                    System.out.print(
                            " -> "
                    );
                }
            }

        } else {

            System.out.print(
                    "Route unavailable"
            );
        }


        System.out.println();


        // -----------------------------------------------------
        // DISPLAY TRAVEL TIME
        // -----------------------------------------------------

        if (travelTime >= 0) {

            System.out.println(
                    "Travel  : "
                            + travelTime
                            + " minutes"
            );

        } else {

            System.out.println(
                    "Travel  : Not available"
            );
        }


        System.out.println(
                "Status  : "
                        + patient.getStatus()
        );


        System.out.println(
                "=============================================="
        );
    }


    // =========================================================
    // DISPLAY WAITING PATIENTS
    // =========================================================

    private void displayWaitingPatients() {

        System.out.println(
                "\n=============================================="
        );

        System.out.println(
                "             WAITING PATIENTS"
        );

        System.out.println(
                "=============================================="
        );


        List<Patient> waitingPatients =
                new ArrayList<>();


        List<Patient> specialistWaiting =
                new ArrayList<>();


        for (Patient patient :
                patientRecords.values()) {


            if (patient.getStatus()
                    .equals("WAITING")) {

                waitingPatients.add(patient);
            }


            if (patient.getStatus()
                    .equals("WAITING_FOR_SPECIALIST")) {

                specialistWaiting.add(patient);
            }
        }


        // =====================================================
        // SORT NORMAL WAITING PATIENTS
        // =====================================================

        waitingPatients.sort(
                new Comparator<Patient>() {

                    @Override
                    public int compare(
                            Patient p1,
                            Patient p2) {

                        int severity1 =
                                getSeverityPriority(
                                        p1.getSeverity()
                                );

                        int severity2 =
                                getSeverityPriority(
                                        p2.getSeverity()
                                );


                        if (severity1 != severity2) {

                            return Integer.compare(
                                    severity2,
                                    severity1
                            );
                        }


                        return Integer.compare(
                                p1.getArrivalOrder(),
                                p2.getArrivalOrder()
                        );
                    }
                }
        );


        // =====================================================
        // NORMAL WAITING
        // =====================================================

        if (waitingPatients.isEmpty()) {

            System.out.println(
                    "\nNo normal waiting patients."
            );

        } else {

            System.out.println(
                    "\n----------- NORMAL WAITING -----------"
            );


            for (Patient patient :
                    waitingPatients) {

                System.out.println(
                        patient
                );

                System.out.println(
                        "--------------------------------------"
                );
            }
        }


        // =====================================================
        // WAITING FOR SPECIALIST
        // =====================================================

        if (specialistWaiting.isEmpty()) {

            System.out.println(
                    "\nNo patients waiting for specialist."
            );

        } else {

            System.out.println(
                    "\n----------- WAITING FOR SPECIALIST -----------"
            );


            for (Patient patient :
                    specialistWaiting) {

                System.out.println(
                        "Patient ID : "
                                + patient.getPatientId()
                );

                System.out.println(
                        "Name       : "
                                + patient.getName()
                );

                System.out.println(
                        "Severity   : "
                                + patient.getSeverity()
                );

                System.out.println(
                        "Hospital   : "
                                + patient.getAssignedHospital()
                );

                System.out.println(
                        "Bed        : "
                                + patient.getAssignedBed()
                );

                System.out.println(
                        "Doctor     : "
                                + patient.getAssignedDoctor()
                );

                System.out.println(
                        "Status     : "
                                + patient.getStatus()
                );

                System.out.println(
                        "--------------------------------------"
                );
            }
        }
    }


    // =========================================================
    // GET SEVERITY PRIORITY
    // =========================================================

    private int getSeverityPriority(
            String severity) {

        if (severity.equals("CRITICAL")) {

            return 3;
        }

        if (severity.equals("SERIOUS")) {

            return 2;
        }

        return 1;
    }


    // =========================================================
    // DISPLAY ALL HOSPITAL RESOURCES
    // =========================================================

    private void displayAllHospitalResources() {

        System.out.println(
                "\n=============================================="
        );

        System.out.println(
                "          ALL HOSPITAL RESOURCES"
        );

        System.out.println(
                "=============================================="
        );


        displaySingleHospitalResources(
                primaryHospital
        );


        for (Hospital hospital :
                referralHospitals) {

            displaySingleHospitalResources(
                    hospital
            );
        }
    }


    // =========================================================
    // DISPLAY SINGLE HOSPITAL RESOURCES
    // =========================================================

    private void displaySingleHospitalResources(
            Hospital hospital) {

        System.out.println(
                "\n----------------------------------------------"
        );

        System.out.println(
                hospital.getHospitalName()
        );

        System.out.println(
                "Hospital ID: "
                        + hospital.getHospitalId()
        );

        System.out.println(
                "----------------------------------------------"
        );


        // -----------------------------------------------------
        // BEDS
        // -----------------------------------------------------

        System.out.println(
                "\nBeds:"
        );


        for (Bed bed :
                hospital.getBeds()) {

            System.out.println(
                    bed
            );
        }


        // -----------------------------------------------------
        // DOCTORS
        // -----------------------------------------------------

        System.out.println(
                "\nDoctors:"
        );


        for (Doctor doctor :
                hospital.getDoctors()) {

            System.out.println(
                    doctor
            );
        }
    }


    // =========================================================
    // VERIFY / SEARCH PATIENT
    // =========================================================

    private void verifySearchPatient() {

        System.out.println(
                "\n=============================================="
        );

        System.out.println(
                "          PATIENT ID VERIFICATION"
        );

        System.out.println(
                "=============================================="
        );


        System.out.print(
                "Enter Patient ID: "
        );


        String id =
                scanner.nextLine()
                        .trim()
                        .toUpperCase();


        if (!id.matches("P\\d{3}")) {

            System.out.println(
                    "\nInvalid patient ID format."
            );

            return;
        }


        Patient patient =
                patientRecords.get(id);


        if (patient == null) {

            System.out.println(
                    "\nPatient not found."
            );

            return;
        }


        // =====================================================
        // DISPLAY PATIENT INFORMATION
        // =====================================================

        System.out.println(
                "\nPatient verified successfully."
        );


        System.out.println(
                "\n----------- PATIENT DETAILS -----------"
        );


        System.out.println(
                "Patient ID       : "
                        + patient.getPatientId()
        );


        System.out.println(
                "Name             : "
                        + patient.getName()
        );


        System.out.println(
                "Severity         : "
                        + patient.getSeverity()
        );


        System.out.println(
                "Required Facility: "
                        + patient.getRequiredFacility()
        );


        System.out.println(
                "Arrival Order    : "
                        + patient.getArrivalOrder()
        );


        System.out.println(
                "Status           : "
                        + patient.getStatus()
        );


        System.out.println(
                "Hospital         : "
                        + patient.getAssignedHospital()
        );


        System.out.println(
                "Bed              : "
                        + patient.getAssignedBed()
        );


        System.out.println(
                "Doctor           : "
                        + patient.getAssignedDoctor()
        );


        System.out.println(
                "---------------------------------------"
        );
    }


    // =========================================================
    // DISPLAY HOSPITAL NETWORK
    // =========================================================

    private void displayNetwork() {

        System.out.println(
                "\n=============================================="
        );

        System.out.println(
                "             HOSPITAL NETWORK"
        );

        System.out.println(
                "=============================================="
        );


        System.out.println(
                "Hospital A <-> Hospital B : 10 minutes"
        );

        System.out.println(
                "Hospital A <-> Location 1 : 5 minutes"
        );

        System.out.println(
                "Hospital A <-> Location 2 : 12 minutes"
        );

        System.out.println(
                "Hospital B <-> Location 1 : 8 minutes"
        );

        System.out.println(
                "Hospital B <-> Location 3 : 6 minutes"
        );

        System.out.println(
                "Hospital C <-> Location 2 : 7 minutes"
        );

        System.out.println(
                "Hospital C <-> Location 3 : 9 minutes"
        );

        System.out.println(
                "Hospital D <-> Location 1 : 10 minutes"
        );

        System.out.println(
                "Hospital D <-> Location 2 : 6 minutes"
        );

        System.out.println(
                "Location 1 <-> Location 3 : 4 minutes"
        );

        System.out.println(
                "Location 2 <-> Location 3 : 5 minutes"
        );

        System.out.println(
                "Location 3 <-> Location 4 : 8 minutes"
        );


        System.out.println(
                "\nDijkstra's algorithm is used"
        );

        System.out.println(
                "to calculate the shortest reachable route."
        );


        System.out.println(
                "=============================================="
        );
    }


    // =========================================================
    // START DOCTOR CONSULTATION
    // =========================================================

    private void startDoctorConsultation() {

        System.out.println(
                "\n=============================================="
        );

        System.out.println(
                "          START DOCTOR CONSULTATION"
        );

        System.out.println(
                "=============================================="
        );


        System.out.print(
                "Enter Patient ID: "
        );


        String patientId =
                scanner.nextLine()
                        .trim()
                        .toUpperCase();


        Patient patient =
                patientRecords.get(patientId);


        if (patient == null) {

            System.out.println(
                    "\nPatient not found."
            );

            return;
        }


        // -----------------------------------------------------
        // PATIENT MUST BE WAITING FOR SPECIALIST
        // -----------------------------------------------------

        if (!patient.getStatus()
                .equals("WAITING_FOR_SPECIALIST")) {

            System.out.println(
                    "\nPatient is not waiting for a specialist."
            );

            System.out.println(
                    "Current status: "
                            + patient.getStatus()
            );

            return;
        }


        // -----------------------------------------------------
        // PATIENT MUST BE AT PRIMARY HOSPITAL
        // -----------------------------------------------------

        if (!patient.getAssignedHospital()
                .equals(primaryHospital.getHospitalName())) {

            System.out.println(
                    "\nPatient is not currently at the primary hospital."
            );

            return;
        }


        // -----------------------------------------------------
        // START CONSULTATION
        // -----------------------------------------------------

        boolean consultationStarted =
                resourceManager.startDoctorConsultation(
                        patient
                );


        if (consultationStarted) {

            System.out.println(
                    "\nDoctor consultation started."
            );

            System.out.println(
                    "Patient : "
                            + patient.getPatientId()
            );

            System.out.println(
                    "Doctor  : "
                            + patient.getAssignedDoctor()
            );

            System.out.println(
                    "Status  : "
                            + patient.getStatus()
            );

        } else {

            System.out.println(
                    "\nNo suitable doctor is currently available."
            );
        }
    }


    // =========================================================
    // FINISH DOCTOR CONSULTATION
    // =========================================================

    private void finishDoctorConsultation() {

        System.out.println(
                "\n=============================================="
        );

        System.out.println(
                "          FINISH DOCTOR CONSULTATION"
        );

        System.out.println(
                "=============================================="
        );


        System.out.print(
                "Enter Doctor ID: "
        );


        String doctorId =
                scanner.nextLine()
                        .trim()
                        .toUpperCase();


        Doctor selectedDoctor = null;


        // -----------------------------------------------------
        // FIND DOCTOR
        // -----------------------------------------------------

        for (Doctor doctor :
                primaryHospital.getDoctors()) {

            if (doctor.getDoctorId()
                    .equals(doctorId)) {

                selectedDoctor = doctor;

                break;
            }
        }


        if (selectedDoctor == null) {

            System.out.println(
                    "\nDoctor not found."
            );

            return;
        }


        // -----------------------------------------------------
        // CHECK DOCTOR STATUS
        // -----------------------------------------------------

        if (selectedDoctor.isAvailable()) {

            System.out.println(
                    "\nDoctor is already available."
            );

            return;
        }


        // -----------------------------------------------------
        // FIND PATIENT BEING CONSULTED
        // -----------------------------------------------------

        Patient consultingPatient = null;


        for (Patient patient :
                patientRecords.values()) {

            if (patient.getStatus()
                    .equals("UNDER_CONSULTATION")
                    && patient.getAssignedDoctor()
                    .equals(selectedDoctor.getName())) {

                consultingPatient = patient;

                break;
            }
        }


        // -----------------------------------------------------
        // FINISH CONSULTATION
        // -----------------------------------------------------

        resourceManager.finishDoctorConsultation(
                doctorId
        );


        if (consultingPatient != null) {

            consultingPatient.setStatus(
                    "ADMITTED"
            );


            System.out.println(
                    "\nConsultation completed."
            );


            System.out.println(
                    "Patient : "
                            + consultingPatient.getPatientId()
            );


            System.out.println(
                    "Doctor  : "
                            + selectedDoctor.getName()
            );


            System.out.println(
                    "Status  : "
                            + consultingPatient.getStatus()
            );

        } else {

            System.out.println(
                    "\nDoctor consultation finished."
            );
        }
    }


    // =========================================================
    // DISCHARGE PATIENT
    // =========================================================

    private void dischargePatient() {

        System.out.println(
                "\n=============================================="
        );

        System.out.println(
                "              DISCHARGE PATIENT"
        );

        System.out.println(
                "=============================================="
        );


        System.out.print(
                "Enter Patient ID: "
        );


        String patientId =
                scanner.nextLine()
                        .trim()
                        .toUpperCase();


        Patient patient =
                patientRecords.get(patientId);


        if (patient == null) {

            System.out.println(
                    "\nPatient not found."
            );

            return;
        }


        // -----------------------------------------------------
        // ONLY ADMITTED PATIENT CAN BE DISCHARGED
        // -----------------------------------------------------

        if (!patient.getStatus()
                .equals("ADMITTED")) {

            System.out.println(
                    "\nPatient cannot be discharged."
            );

            System.out.println(
                    "Current status: "
                            + patient.getStatus()
            );

            return;
        }


        String assignedHospital =
                patient.getAssignedHospital();


        Hospital hospitalForDischarge = null;


        // -----------------------------------------------------
        // FIND HOSPITAL
        // -----------------------------------------------------

        if (assignedHospital
                .equals(primaryHospital.getHospitalName())) {

            hospitalForDischarge =
                    primaryHospital;

        } else {

            for (Hospital hospital :
                    referralHospitals) {

                if (hospital.getHospitalName()
                        .equals(assignedHospital)) {

                    hospitalForDischarge =
                            hospital;

                    break;
                }
            }
        }


        if (hospitalForDischarge == null) {

            System.out.println(
                    "\nAssigned hospital could not be found."
            );

            return;
        }


        // -----------------------------------------------------
        // RELEASE RESOURCE
        // -----------------------------------------------------

        ResourceManager dischargeManager =
                new ResourceManager(
                        hospitalForDischarge
                );


        boolean discharged =
                dischargeManager.dischargePatient(
                        patient
                );


        if (discharged) {

            System.out.println(
                    "\nPatient discharged successfully."
            );


            System.out.println(
                    "Patient ID : "
                            + patient.getPatientId()
            );


            System.out.println(
                    "Hospital   : "
                            + patient.getAssignedHospital()
            );


            System.out.println(
                    "Bed        : "
                            + patient.getAssignedBed()
            );


            System.out.println(
                    "Status     : "
                            + patient.getStatus()
            );


            System.out.println(
                    "\nThe bed is now available."
            );

        } else {

            System.out.println(
                    "\nPatient discharge failed."
            );
        }
    }


    // =========================================================
    // RESOURCE SUMMARY
    // =========================================================

    private void displayResourceSummary() {

        System.out.println(
                "\n=============================================="
        );

        System.out.println(
                "              RESOURCE SUMMARY"
        );

        System.out.println(
                "=============================================="
        );


        displaySingleResourceSummary(
                primaryHospital
        );


        for (Hospital hospital :
                referralHospitals) {

            displaySingleResourceSummary(
                    hospital
            );
        }


        System.out.println(
                "=============================================="
        );
    }


    // =========================================================
    // SINGLE HOSPITAL RESOURCE SUMMARY
    // =========================================================

    private void displaySingleResourceSummary(
            Hospital hospital) {

        int icuTotal = 0;

        int icuAvailable = 0;

        int generalTotal = 0;

        int generalAvailable = 0;


        int icuDoctors = 0;

        int icuDoctorsAvailable = 0;

        int generalDoctors = 0;

        int generalDoctorsAvailable = 0;


        // =====================================================
        // COUNT BEDS
        // =====================================================

        for (Bed bed :
                hospital.getBeds()) {

            if (bed.getType()
                    .equals("ICU")) {

                icuTotal++;

                if (bed.isAvailable()) {

                    icuAvailable++;
                }

            } else {

                generalTotal++;

                if (bed.isAvailable()) {

                    generalAvailable++;
                }
            }
        }


        // =====================================================
        // COUNT DOCTORS
        // =====================================================

        for (Doctor doctor :
                hospital.getDoctors()) {

            if (doctor.getSpecialization()
                    .equals("ICU")) {

                icuDoctors++;

                if (doctor.isAvailable()) {

                    icuDoctorsAvailable++;
                }

            } else {

                generalDoctors++;

                if (doctor.isAvailable()) {

                    generalDoctorsAvailable++;
                }
            }
        }


        // =====================================================
        // DISPLAY
        // =====================================================

        System.out.println(
                "\n----------------------------------------------"
        );


        System.out.println(
                hospital.getHospitalName()
        );


        System.out.println(
                "----------------------------------------------"
        );


        System.out.println(
                "\nICU Beds:"
        );

        System.out.println(
                "Total    : "
                        + icuTotal
        );

        System.out.println(
                "Available: "
                        + icuAvailable
        );

        System.out.println(
                "Occupied : "
                        + (icuTotal - icuAvailable)
        );


        System.out.println(
                "\nGeneral Beds:"
        );

        System.out.println(
                "Total    : "
                        + generalTotal
        );

        System.out.println(
                "Available: "
                        + generalAvailable
        );

        System.out.println(
                "Occupied : "
                        + (generalTotal - generalAvailable)
        );


        System.out.println(
                "\nICU Doctors:"
        );

        System.out.println(
                "Total    : "
                        + icuDoctors
        );

        System.out.println(
                "Available: "
                        + icuDoctorsAvailable
        );

        System.out.println(
                "Busy     : "
                        + (icuDoctors - icuDoctorsAvailable)
        );


        System.out.println(
                "\nGeneral Doctors:"
        );

        System.out.println(
                "Total    : "
                        + generalDoctors
        );

        System.out.println(
                "Available: "
                        + generalDoctorsAvailable
        );

        System.out.println(
                "Busy     : "
                        + (generalDoctors - generalDoctorsAvailable)
        );
    }
}