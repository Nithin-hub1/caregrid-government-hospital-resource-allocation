package com.hospital.resourceallocation.controller;

import com.hospital.resourceallocation.entity.Hospital;
import com.hospital.resourceallocation.entity.Patient;
import com.hospital.resourceallocation.repository.BedRepository;
import com.hospital.resourceallocation.repository.HospitalRepository;
import com.hospital.resourceallocation.repository.PatientRepository;
import com.hospital.resourceallocation.security.SecurityUtils;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientRepository patientRepository;
    private final HospitalRepository hospitalRepository;
    private final BedRepository bedRepository;

    public PatientController(
            PatientRepository patientRepository,
            HospitalRepository hospitalRepository,
            BedRepository bedRepository) {
        this.patientRepository = patientRepository;
        this.hospitalRepository = hospitalRepository;
        this.bedRepository = bedRepository;
    }

    @PostMapping
    public ResponseEntity<Patient> registerPatient(
            @Valid @RequestBody Patient patient) {

        if (SecurityUtils.isHospitalOperator()) {
            Long userHospitalId = SecurityUtils.getCurrentHospitalId()
                    .orElseThrow(() -> new AccessDeniedException("No hospital assigned to operator"));
            Hospital operatorHospital = hospitalRepository.findById(userHospitalId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assigned hospital not found"));
            patient.setHospital(operatorHospital);
        } else if (patient.getHospital() != null && patient.getHospital().getId() != null) {
            Hospital hospital = hospitalRepository.findById(patient.getHospital().getId())
                    .orElse(null);
            patient.setHospital(hospital);
        }

        Patient savedPatient = patientRepository.save(patient);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedPatient);
    }

    @GetMapping
    public List<Patient> getAllPatients() {
        if (SecurityUtils.isHospitalOperator()) {
            Long userHospitalId = SecurityUtils.getCurrentHospitalId()
                    .orElseThrow(() -> new AccessDeniedException("No hospital assigned to operator"));
            return patientRepository.findAllByHospitalOrAssignedBedHospital(userHospitalId);
        }
        return patientRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(
            @PathVariable Long id) {

        Patient patient = patientRepository.findById(id).orElse(null);
        if (patient == null) {
            return ResponseEntity.notFound().build();
        }

        if (SecurityUtils.isHospitalOperator()) {
            Long patientHospitalId = resolvePatientHospitalId(patient);
            if (patientHospitalId != null) {
                SecurityUtils.validateHospitalAccess(patientHospitalId);
            }
        }

        return ResponseEntity.ok(patient);
    }

    private Long resolvePatientHospitalId(Patient patient) {
        if (patient.getHospital() != null && patient.getHospital().getId() != null) {
            return patient.getHospital().getId();
        }
        if (patient.getAssignedBedId() != null) {
            return bedRepository.findById(patient.getAssignedBedId())
                    .map(b -> b.getHospital() != null ? b.getHospital().getId() : null)
                    .orElse(null);
        }
        return null;
    }
}
