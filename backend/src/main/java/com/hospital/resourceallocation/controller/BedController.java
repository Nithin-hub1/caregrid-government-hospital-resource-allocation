package com.hospital.resourceallocation.controller;

import com.hospital.resourceallocation.entity.Bed;
import com.hospital.resourceallocation.entity.Hospital;
import com.hospital.resourceallocation.repository.BedRepository;
import com.hospital.resourceallocation.repository.HospitalRepository;
import com.hospital.resourceallocation.security.SecurityUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/beds")
public class BedController {

    private final BedRepository bedRepository;
    private final HospitalRepository hospitalRepository;

    public BedController(
            BedRepository bedRepository,
            HospitalRepository hospitalRepository) {
        this.bedRepository = bedRepository;
        this.hospitalRepository = hospitalRepository;
    }

    public record BedRequest(
            @NotBlank String bedNumber,
            @NotBlank String type,
            @NotNull Long hospitalId) {
    }

    public record BedResponse(
            Long id,
            String bedNumber,
            String type,
            boolean available,
            Long hospitalId) {
    }

    @PostMapping
    public ResponseEntity<?> createBed(
            @Valid @RequestBody BedRequest request) {

        SecurityUtils.validateHospitalAccess(request.hospitalId());

        String bedNumber = request.bedNumber().trim();
        String type = normalizeType(request.type());

        if (bedNumber.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Bed number is required"));
        }

        if (!isValidType(type)) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Bed type must be ICU or GENERAL"));
        }

        var hospitalOptional =
                hospitalRepository.findById(request.hospitalId());

        if (hospitalOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "Hospital not found"));
        }

        Hospital hospital = hospitalOptional.get();

        Bed bed = new Bed();
        bed.setBedNumber(bedNumber);
        bed.setType(type);
        bed.setAvailable(true);
        bed.setHospital(hospital);

        Bed saved = bedRepository.save(bed);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                toResponse(saved, hospital.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateBed(
            @PathVariable Long id,
            @Valid @RequestBody BedRequest request) {

        var bedOptional = bedRepository.findById(id);

        if (bedOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "Bed not found"));
        }

        Bed bed = bedOptional.get();
        if (bed.getHospital() != null) {
            SecurityUtils.validateHospitalAccess(bed.getHospital().getId());
        }
        SecurityUtils.validateHospitalAccess(request.hospitalId());

        String bedNumber = request.bedNumber().trim();
        String type = normalizeType(request.type());

        if (bedNumber.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Bed number is required"));
        }

        if (!isValidType(type)) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Bed type must be ICU or GENERAL"));
        }

        var hospitalOptional =
                hospitalRepository.findById(request.hospitalId());

        if (hospitalOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "Hospital not found"));
        }

        Hospital newHospital = hospitalOptional.get();

        // Never move an occupied bed or change its type.
        // This protects active patient allocations.
        if (!bed.isAvailable()) {
            boolean changingHospital =
                    !bed.getHospital().getId().equals(newHospital.getId());

            boolean changingType = !bed.getType().equals(type);

            if (changingHospital || changingType) {
                return ResponseEntity.badRequest().body(
                        Map.of(
                                "error",
                                "An occupied bed cannot change hospital or type. "
                                        + "Discharge the patient first."));
            }
        }

        // Update editable details without changing availability.
        bed.setBedNumber(bedNumber);
        bed.setType(type);
        bed.setHospital(newHospital);

        Bed saved = bedRepository.save(bed);

        return ResponseEntity.ok(
                toResponse(saved, newHospital.getId()));
    }

    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<?> getBedsByHospital(
            @PathVariable Long hospitalId) {

        SecurityUtils.validateHospitalAccess(hospitalId);

        if (!hospitalRepository.existsById(hospitalId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "Hospital not found"));
        }

        List<BedResponse> beds = bedRepository
                .findByHospital_Id(hospitalId)
                .stream()
                .map(bed -> toResponse(bed, hospitalId))
                .toList();

        return ResponseEntity.ok(beds);
    }

    private BedResponse toResponse(Bed bed, Long hospitalId) {
        return new BedResponse(
                bed.getId(),
                bed.getBedNumber(),
                bed.getType(),
                bed.isAvailable(),
                hospitalId);
    }

    private String normalizeType(String type) {
        return type.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isValidType(String type) {
        return type.equals("ICU") || type.equals("GENERAL");
    }
}
