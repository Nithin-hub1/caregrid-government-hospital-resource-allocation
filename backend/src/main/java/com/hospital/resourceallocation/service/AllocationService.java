package com.hospital.resourceallocation.service;

import com.hospital.resourceallocation.entity.Bed;
import com.hospital.resourceallocation.entity.Doctor;
import com.hospital.resourceallocation.entity.Hospital;
import com.hospital.resourceallocation.entity.Patient;
import com.hospital.resourceallocation.repository.BedRepository;
import com.hospital.resourceallocation.repository.DoctorRepository;
import com.hospital.resourceallocation.repository.HospitalRepository;
import com.hospital.resourceallocation.repository.PatientRepository;
import com.hospital.resourceallocation.security.SecurityUtils;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class AllocationService {

    private final PatientRepository patientRepository;
    private final BedRepository bedRepository;
    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;
    private final ReferralService referralService;

    public AllocationService(
            PatientRepository patientRepository,
            BedRepository bedRepository,
            DoctorRepository doctorRepository,
            HospitalRepository hospitalRepository,
            ReferralService referralService) {
        this.patientRepository = patientRepository;
        this.bedRepository = bedRepository;
        this.doctorRepository = doctorRepository;
        this.hospitalRepository = hospitalRepository;
        this.referralService = referralService;
    }

    public record AllocationResponse(
            Long patientId,
            String patientName,
            String severity,
            String status,
            Long bedId,
            String bedNumber,
            String bedType,
            Long hospitalId,
            Long doctorId,
            String doctorName,
            String message) {
    }

    @Transactional
    public AllocationResponse allocatePatient(Long patientId) {

        Patient patient = patientRepository.findByIdForUpdate(patientId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Patient not found: " + patientId));

        if (SecurityUtils.isHospitalOperator()) {
            Long operatorHospitalId = SecurityUtils.getCurrentHospitalId().orElse(null);
            if (operatorHospitalId != null) {
                Long patientHospitalId = patient.getHospital() != null ? patient.getHospital().getId() : null;
                if (patientHospitalId != null && !patientHospitalId.equals(operatorHospitalId)) {
                    throw new AccessDeniedException(
                            "Access denied: Hospital Operator is only authorized to allocate beds for patients registered at hospital ID "
                                    + operatorHospitalId);
                }
            }
        }

        if (patient.getAssignedBedId() != null
                || "ADMITTED".equalsIgnoreCase(patient.getStatus())) {
            throw new IllegalStateException(
                    "Patient already has an allocated bed.");
        }

        String status = patient.getStatus();

        if (status == null
                || (!"REGISTERED".equalsIgnoreCase(status)
                && !"WAITING".equalsIgnoreCase(status))) {
            throw new IllegalStateException(
                    "Only REGISTERED or WAITING patients can be allocated a bed. Current status: " + status);
        }

        if (patient.getSeverity() == null
                || patient.getSeverity().isBlank()) {
            throw new IllegalArgumentException(
                    "Patient severity is required.");
        }

        String severity = patient.getSeverity()
                .trim()
                .toUpperCase(Locale.ROOT);

        String requiredType = switch (severity) {
            case "CRITICAL" -> "ICU";
            case "SERIOUS", "MINOR" -> "GENERAL";
            default -> throw new IllegalArgumentException(
                    "Severity must be CRITICAL, SERIOUS, or MINOR.");
        };

        // Identify local hospital: use patient's designated hospital, or designated primary, or fallback
        Hospital initialHospital = patient.getHospital();
        if (initialHospital == null) {
            initialHospital = hospitalRepository.findFirstByPrimaryHospitalTrue()
                    .orElseGet(() -> hospitalRepository.findAll().stream().findFirst().orElse(null));
        }

        if (initialHospital != null && initialHospital.isActive()) {
            // Check initial hospital first
            List<Bed> primaryBeds = bedRepository.findAvailableBedsByHospitalForUpdate(
                    initialHospital.getId(), requiredType);

            if (!primaryBeds.isEmpty()) {
                Doctor primaryDoctor = findAppropriateAvailableDoctor(
                        initialHospital.getId(), requiredType);

                if (primaryDoctor != null) {
                    Bed bed = primaryBeds.get(0);
                    bed.setAvailable(false);
                    primaryDoctor.setAvailable(false);
                    primaryDoctor.setAssignedPatient(patient);

                    patient.setAssignedBedId(bed.getId());
                    patient.setStatus("ADMITTED");
                    patient.setHospital(initialHospital);

                    bedRepository.save(bed);
                    doctorRepository.save(primaryDoctor);
                    patientRepository.save(patient);

                    return new AllocationResponse(
                            patient.getId(),
                            patient.getPatientName(),
                            severity,
                            "ADMITTED",
                            bed.getId(),
                            bed.getBedNumber(),
                            bed.getType(),
                            initialHospital.getId(),
                            primaryDoctor.getId(),
                            primaryDoctor.getName(),
                            "Patient admitted locally to " + initialHospital.getName()
                                    + ". Bed " + bed.getBedNumber() + " and doctor " + primaryDoctor.getName() + " assigned."
                    );
                }
            }
        }

        // Initial hospital cannot admit the patient.
        // Find fastest reachable eligible referral hospital in the network using Dijkstra.
        Long sourceHospitalId = initialHospital != null ? initialHospital.getId() : 1L;
        Optional<ReferralService.Candidate> bestCandidate =
                referralService.findBestReferralCandidate(patient, sourceHospitalId);

        if (bestCandidate.isPresent()) {
            ReferralService.Candidate candidate = bestCandidate.get();
            Hospital targetHospital = candidate.hospital();
            Bed targetBed = candidate.bed();
            Doctor targetDoctor = candidate.doctor();

            // Reserve referral hospital resources safely
            targetBed.setAvailable(false);
            targetDoctor.setAvailable(false);
            targetDoctor.setAssignedPatient(patient);

            patient.setAssignedBedId(targetBed.getId());
            patient.setStatus("REFERRED");

            bedRepository.save(targetBed);
            doctorRepository.save(targetDoctor);
            patientRepository.save(patient);

            return new AllocationResponse(
                    patient.getId(),
                    patient.getPatientName(),
                    severity,
                    "REFERRED",
                    targetBed.getId(),
                    targetBed.getBedNumber(),
                    targetBed.getType(),
                    targetHospital.getId(),
                    targetDoctor.getId(),
                    targetDoctor.getName(),
                    "Local hospital resources unavailable. Patient referred and resources reserved at "
                            + targetHospital.getName() + " (Bed: " + targetBed.getBedNumber()
                            + ", Doctor: " + targetDoctor.getName() + "). Estimated travel time: "
                            + candidate.travelMinutes() + " mins."
            );
        }

        // Neither local nor referral hospitals can admit. Keep patient in WAITING queue.
        patient.setStatus("WAITING");
        patient.setAssignedBedId(null);
        if (patient.getHospital() == null && initialHospital != null) {
            patient.setHospital(initialHospital);
        }
        patientRepository.save(patient);

        return new AllocationResponse(
                patient.getId(),
                patient.getPatientName(),
                severity,
                "WAITING",
                null,
                null,
                requiredType,
                null,
                null,
                null,
                "No hospital in the network currently has both a compatible available bed and an available doctor. "
                        + "Patient placed in priority waiting queue."
        );
    }

    private Doctor findAppropriateAvailableDoctor(Long hospitalId, String requiredType) {
        String keyword = "ICU".equalsIgnoreCase(requiredType) ? "ICU" : "GENERAL";
        List<Doctor> specDoctors = doctorRepository.findAvailableDoctorsBySpecializationForUpdate(
                hospitalId, keyword);

        if (!specDoctors.isEmpty()) {
            return specDoctors.get(0);
        }

        List<Doctor> anyDoctors = doctorRepository.findAvailableDoctorsForUpdate(hospitalId);
        return anyDoctors.isEmpty() ? null : anyDoctors.get(0);
    }
}
