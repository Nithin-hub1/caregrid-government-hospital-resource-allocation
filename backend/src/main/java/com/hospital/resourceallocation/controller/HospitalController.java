
package com.hospital.resourceallocation.controller;

import com.hospital.resourceallocation.entity.Hospital;
import com.hospital.resourceallocation.repository.HospitalRepository;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {

    private final HospitalRepository hospitalRepository;

    public HospitalController(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    @PostMapping
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Hospital> createHospital(
            @Valid @RequestBody Hospital hospital) {

        Hospital saved = hospitalRepository.save(hospital);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Hospital> getAllHospitals() {
        return hospitalRepository.findAll();
    }

    @GetMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ResponseEntity<Hospital> getHospitalById(
            @PathVariable Long id) {

        return hospitalRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
