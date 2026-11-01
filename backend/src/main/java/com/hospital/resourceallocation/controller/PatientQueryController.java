package com.hospital.resourceallocation.controller;

import com.hospital.resourceallocation.entity.Patient;
import com.hospital.resourceallocation.repository.PatientRepository;
import com.hospital.resourceallocation.security.SecurityUtils;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/patients")
public class PatientQueryController {

    private final PatientRepository patientRepository;

    private static final Set<String> VALID_SEVERITIES =
            Set.of("CRITICAL", "SERIOUS", "MINOR");

    private static final Set<String> VALID_STATUSES =
            Set.of("REGISTERED", "WAITING", "ADMITTED", "REFERRED", "DISCHARGED");

    public PatientQueryController(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    /*
     * Search patients by optional name, severity, and status.
     * Restricts results to the operator's assigned hospital if authenticated as HOSPITAL_OPERATOR.
     */
    @GetMapping("/search")
    public List<Patient> searchPatients(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status) {

        String normalizedSeverity = normalizeFilter(
                severity, VALID_SEVERITIES, "severity");

        String normalizedStatus = normalizeFilter(
                status, VALID_STATUSES, "status");

        String normalizedName = name == null
                ? null
                : name.trim().toLowerCase();

        List<Patient> basePatients;
        if (SecurityUtils.isHospitalOperator()) {
            Long userHospitalId = SecurityUtils.getCurrentHospitalId()
                    .orElseThrow(() -> new AccessDeniedException("No hospital assigned to operator"));
            basePatients = patientRepository.findAllByHospitalOrAssignedBedHospital(userHospitalId);
        } else {
            basePatients = patientRepository.findAll();
        }

        return basePatients
                .stream()
                .filter(patient ->
                        normalizedName == null
                                || normalizedName.isBlank()
                                || patient.getPatientName()
                                        .toLowerCase()
                                        .contains(normalizedName))
                .filter(patient ->
                        normalizedSeverity == null
                                || normalizedSeverity.equalsIgnoreCase(
                                        patient.getSeverity()))
                .filter(patient ->
                        normalizedStatus == null
                                || normalizedStatus.equalsIgnoreCase(
                                        patient.getStatus()))
                .sorted(Comparator.comparing(Patient::getId))
                .toList();
    }

    /*
     * Return waiting patients in priority order:
     * CRITICAL -> SERIOUS -> MINOR.
     * Restricted to the operator's hospital waiting queue if authenticated as HOSPITAL_OPERATOR.
     */
    @GetMapping("/waiting-queue")
    public List<Patient> getWaitingQueue() {

        List<Patient> waitingPatients;
        if (SecurityUtils.isHospitalOperator()) {
            Long userHospitalId = SecurityUtils.getCurrentHospitalId()
                    .orElseThrow(() -> new AccessDeniedException("No hospital assigned to operator"));
            waitingPatients = patientRepository.findWaitingQueueByHospital(userHospitalId);
        } else {
            waitingPatients = patientRepository.findByStatus("WAITING");
        }

        return waitingPatients
                .stream()
                .sorted(
                        Comparator
                                .comparingInt(
                                        (Patient patient) ->
                                                severityRank(
                                                        patient.getSeverity()))
                                .thenComparing(
                                        Patient::getArrivalTime,
                                        Comparator.nullsLast(
                                                Comparator.naturalOrder()))
                                .thenComparing(Patient::getId)
                )
                .toList();
    }

    private String normalizeFilter(
            String value,
            Set<String> allowedValues,
            String fieldName) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim().toUpperCase();

        if (!allowedValues.contains(normalized)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid " + fieldName + ". Allowed values: "
                            + allowedValues);
        }

        return normalized;
    }

    private int severityRank(String severity) {

        if (severity == null) {
            return 4;
        }

        return switch (severity.trim().toUpperCase()) {
            case "CRITICAL" -> 1;
            case "SERIOUS" -> 2;
            case "MINOR" -> 3;
            default -> 4;
        };
    }
}
