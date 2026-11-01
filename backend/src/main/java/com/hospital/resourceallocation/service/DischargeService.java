package com.hospital.resourceallocation.service;

import com.hospital.resourceallocation.entity.Bed;
import com.hospital.resourceallocation.entity.Doctor;
import com.hospital.resourceallocation.entity.Patient;
import com.hospital.resourceallocation.repository.BedRepository;
import com.hospital.resourceallocation.repository.DoctorRepository;
import com.hospital.resourceallocation.repository.PatientRepository;
import com.hospital.resourceallocation.security.SecurityUtils;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DischargeService {

    private final PatientRepository patientRepository;
    private final BedRepository bedRepository;
    private final DoctorRepository doctorRepository;
    private final WaitingQueueService waitingQueueService;

    public DischargeService(
            PatientRepository patientRepository,
            BedRepository bedRepository,
            DoctorRepository doctorRepository,
            WaitingQueueService waitingQueueService) {
        this.patientRepository = patientRepository;
        this.bedRepository = bedRepository;
        this.doctorRepository = doctorRepository;
        this.waitingQueueService = waitingQueueService;
    }

    public record DischargeResponse(
            Long patientId,
            String patientName,
            String status,
            Long releasedBedId,
            Long releasedDoctorId,
            Long newlyAdmittedPatientId,
            Long newlyAssignedDoctorId,
            String message) {
    }

    @Transactional
    public DischargeResponse dischargePatient(Long patientId) {

        Patient patient = patientRepository.findByIdForUpdate(patientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Patient not found. ID: " + patientId));

        if ((!"ADMITTED".equalsIgnoreCase(patient.getStatus())
                && !"REFERRED".equalsIgnoreCase(patient.getStatus()))
                || patient.getAssignedBedId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Patient is not currently admitted or referred to a bed.");
        }

        Long bedId = patient.getAssignedBedId();

        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new IllegalStateException(
                        "Assigned bed was not found. Bed ID: " + bedId));

        // Enforce boundary check: Hospital Operators can only discharge patients in their hospital
        if (bed.getHospital() != null && bed.getHospital().getId() != null) {
            SecurityUtils.validateHospitalAccess(bed.getHospital().getId());
        }

        if (bed.isAvailable()) {
            throw new IllegalStateException(
                    "Data inconsistency: assigned bed is already available.");
        }

        // Find the doctor's assignment, if this patient has one.
        List<Doctor> assignedDoctors =
                doctorRepository.findByAssignedPatient_Id(patientId);

        Long releasedDoctorId = null;

        for (Doctor doctor : assignedDoctors) {
            doctor.setAssignedPatient(null);
            doctor.setAvailable(true);
            doctorRepository.save(doctor);

            if (releasedDoctorId == null) {
                releasedDoctorId = doctor.getId();
            }
        }

        // Discharge the patient and release the bed.
        // Maintain hospital association for discharged patient records
        if (patient.getHospital() == null && bed.getHospital() != null) {
            patient.setHospital(bed.getHospital());
        }
        patient.setStatus("DISCHARGED");
        patient.setAssignedBedId(null);

        bed.setAvailable(true);

        patientRepository.save(patient);
        bedRepository.save(bed);

        // Automatically admit a compatible waiting patient, if possible.
        WaitingQueueService.QueueAssignment assignment =
                waitingQueueService.assignNextPatient(bed);

        String message;

        if (assignment != null) {
            message = "Patient discharged. Bed "
                    + bed.getBedNumber()
                    + " and doctor "
                    + assignment.doctorName()
                    + " were assigned to "
                    + assignment.patientName()
                    + " (Patient ID: "
                    + assignment.patientId()
                    + ").";
        } else {
            message = "Patient discharged successfully. "
                    + "The bed remains available if no compatible "
                    + "waiting patient and available doctor could be assigned.";
        }

        return new DischargeResponse(
                patient.getId(),
                patient.getPatientName(),
                patient.getStatus(),
                bedId,
                releasedDoctorId,
                assignment == null ? null : assignment.patientId(),
                assignment == null ? null : assignment.doctorId(),
                message);
    }
}
