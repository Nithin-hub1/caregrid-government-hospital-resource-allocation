package com.hospital.resourceallocation.controller;

import com.hospital.resourceallocation.entity.Doctor;
import com.hospital.resourceallocation.entity.Hospital;
import com.hospital.resourceallocation.repository.DoctorRepository;
import com.hospital.resourceallocation.repository.HospitalRepository;
import com.hospital.resourceallocation.security.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;

    public DoctorController(
            DoctorRepository doctorRepository,
            HospitalRepository hospitalRepository) {
        this.doctorRepository = doctorRepository;
        this.hospitalRepository = hospitalRepository;
    }

    public record DoctorRequest(
            String name,
            String specialization,
            Long hospitalId
    ) {}

    public record DoctorResponse(
            Long id,
            String name,
            String specialization,
            boolean available,
            Long hospitalId,
            String hospitalName,
            Long assignedPatientId
    ) {}

    // GET /api/doctors
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<DoctorResponse>> getAllDoctors() {
        if (SecurityUtils.isHospitalOperator()) {
            Long userHospitalId = SecurityUtils.getCurrentHospitalId()
                    .orElseThrow(() -> new AccessDeniedException("No hospital assigned to operator"));
            List<DoctorResponse> doctors = doctorRepository.findByHospital_Id(userHospitalId)
                    .stream()
                    .map(this::toResponse)
                    .toList();
            return ResponseEntity.ok(doctors);
        }

        List<DoctorResponse> doctors = doctorRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(doctors);
    }

    // GET /api/doctors/hospital/{hospitalId}
    @GetMapping("/hospital/{hospitalId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getDoctorsByHospital(
            @PathVariable Long hospitalId) {

        SecurityUtils.validateHospitalAccess(hospitalId);

        if (!hospitalRepository.existsById(hospitalId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Hospital not found: " + hospitalId);
        }

        List<DoctorResponse> doctors = doctorRepository.findByHospital_Id(hospitalId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(doctors);
    }

    // POST /api/doctors
    @PostMapping
    @Transactional
    public ResponseEntity<?> addDoctor(
            @RequestBody DoctorRequest request) {

        if (request == null
                || request.name() == null
                || request.name().isBlank()
                || request.specialization() == null
                || request.specialization().isBlank()
                || request.hospitalId() == null) {

            return ResponseEntity.badRequest()
                    .body("Name, specialization and hospital are required.");
        }

        SecurityUtils.validateHospitalAccess(request.hospitalId());

        Hospital hospital = hospitalRepository
                .findById(request.hospitalId())
                .orElse(null);

        if (hospital == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Hospital not found. Please select a valid hospital.");
        }

        Doctor doctor = new Doctor();
        doctor.setName(request.name().trim());
        doctor.setSpecialization(
                request.specialization().trim().toUpperCase(Locale.ROOT));
        doctor.setHospital(hospital);
        doctor.setAvailable(true);

        Doctor saved = doctorRepository.save(doctor);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(saved));
    }

    // PUT /api/doctors/{id}
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> updateDoctor(
            @PathVariable Long id,
            @RequestBody DoctorRequest request) {

        if (request == null
                || request.name() == null
                || request.name().isBlank()
                || request.specialization() == null
                || request.specialization().isBlank()
                || request.hospitalId() == null) {

            return ResponseEntity.badRequest()
                    .body("Name, specialization and hospital are required.");
        }

        Doctor doctor = doctorRepository.findById(id).orElse(null);

        if (doctor == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Doctor not found: " + id);
        }

        if (doctor.getHospital() != null) {
            SecurityUtils.validateHospitalAccess(doctor.getHospital().getId());
        }
        SecurityUtils.validateHospitalAccess(request.hospitalId());

        Hospital hospital = hospitalRepository
                .findById(request.hospitalId())
                .orElse(null);

        if (hospital == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Hospital not found: " + request.hospitalId());
        }

        if (doctor.getAssignedPatient() != null
                && (doctor.getHospital() == null
                || !doctor.getHospital().getId().equals(hospital.getId()))) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Cannot move a doctor who is assigned to a patient.");
        }

        doctor.setName(request.name().trim());
        doctor.setSpecialization(
                request.specialization().trim().toUpperCase(Locale.ROOT));

        if (doctor.getAssignedPatient() == null) {
            doctor.setHospital(hospital);
        }

        Doctor updated = doctorRepository.save(doctor);

        return ResponseEntity.ok(toResponse(updated));
    }

    private DoctorResponse toResponse(Doctor doctor) {
        Hospital hospital = doctor.getHospital();

        Long patientId = doctor.getAssignedPatient() == null
                ? null
                : doctor.getAssignedPatient().getId();

        return new DoctorResponse(
                doctor.getId(),
                doctor.getName(),
                doctor.getSpecialization(),
                doctor.isAvailable(),
                hospital == null ? null : hospital.getId(),
                hospital == null ? null : hospital.getName(),
                patientId
        );
    }
}
