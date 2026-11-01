
package com.hospital.resourceallocation.service;

import com.hospital.resourceallocation.entity.Bed;
import com.hospital.resourceallocation.entity.Doctor;
import com.hospital.resourceallocation.entity.Patient;
import com.hospital.resourceallocation.repository.BedRepository;
import com.hospital.resourceallocation.repository.DoctorRepository;
import com.hospital.resourceallocation.repository.PatientRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class WaitingQueueService {

    private final PatientRepository patientRepository;
    private final BedRepository bedRepository;
    private final DoctorRepository doctorRepository;

    public WaitingQueueService(
            PatientRepository patientRepository,
            BedRepository bedRepository,
            DoctorRepository doctorRepository) {
        this.patientRepository = patientRepository;
        this.bedRepository = bedRepository;
        this.doctorRepository = doctorRepository;
    }

    public record QueueAssignment(
            Long patientId,
            String patientName,
            String severity,
            Long bedId,
            String bedNumber,
            Long doctorId,
            String doctorName,
            String message) {
    }

    @Transactional
    public QueueAssignment assignNextPatient(Bed bed) {

        if (!bed.isAvailable()) {
            throw new IllegalStateException(
                    "The bed is no longer available.");
        }

        List<Patient> waitingPatients =
                new ArrayList<>(patientRepository.findByStatus("WAITING"));

        waitingPatients.sort(
                Comparator
                        .comparingInt(
                                (Patient patient) ->
                                        getPriority(patient.getSeverity()))
                        .thenComparing(
                                Patient::getArrivalTime,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()))
                        .thenComparing(
                                Patient::getId,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()))
        );

        String bedType = bed.getType();
        Long hospitalId = bed.getHospital().getId();

        for (Patient patient : waitingPatients) {

            String requiredBedType =
                    getRequiredBedType(patient.getSeverity());

            if (requiredBedType == null
                    || !requiredBedType.equalsIgnoreCase(bedType)) {
                continue;
            }

            String keyword = "ICU".equalsIgnoreCase(bedType) ? "ICU" : "GENERAL";
            List<Doctor> specDoctors =
                    doctorRepository.findAvailableDoctorsBySpecializationForUpdate(hospitalId, keyword);

            Doctor doctor = !specDoctors.isEmpty()
                    ? specDoctors.get(0)
                    : (!doctorRepository.findAvailableDoctorsForUpdate(hospitalId).isEmpty()
                    ? doctorRepository.findAvailableDoctorsForUpdate(hospitalId).get(0)
                    : null);

            if (doctor == null) {
                // Keep this bed available and the patient waiting.
                return null;
            }

            if (!bed.isAvailable() || !doctor.isAvailable()) {
                continue;
            }

            bed.setAvailable(false);

            patient.setAssignedBedId(bed.getId());
            patient.setStatus("ADMITTED");

            doctor.setAvailable(false);
            doctor.setAssignedPatient(patient);

            bedRepository.save(bed);
            doctorRepository.save(doctor);
            patientRepository.save(patient);

            return new QueueAssignment(
                    patient.getId(),
                    patient.getPatientName(),
                    patient.getSeverity(),
                    bed.getId(),
                    bed.getBedNumber(),
                    doctor.getId(),
                    doctor.getName(),
                    "Waiting patient admitted with a doctor assignment.");
        }

        return null;
    }

    private int getPriority(String severity) {

        if (severity == null) {
            return 99;
        }

        return switch (severity.trim().toUpperCase(Locale.ROOT)) {
            case "CRITICAL" -> 1;
            case "SERIOUS" -> 2;
            case "MINOR" -> 3;
            default -> 99;
        };
    }

    private String getRequiredBedType(String severity) {

        if (severity == null) {
            return null;
        }

        return switch (severity.trim().toUpperCase(Locale.ROOT)) {
            case "CRITICAL" -> "ICU";
            case "SERIOUS", "MINOR" -> "GENERAL";
            default -> null;
        };
    }
}
