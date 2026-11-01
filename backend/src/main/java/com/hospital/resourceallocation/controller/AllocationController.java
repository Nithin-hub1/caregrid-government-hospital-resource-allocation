
package com.hospital.resourceallocation.controller;

import com.hospital.resourceallocation.service.AllocationService;
import com.hospital.resourceallocation.service.AllocationService.AllocationResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/allocations")
public class AllocationController {

    private final AllocationService allocationService;

    public AllocationController(
            AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @PostMapping("/patients/{patientId}")
    public ResponseEntity<?> allocatePatient(
            @PathVariable Long patientId) {

        try {
            AllocationResponse result =
                    allocationService.allocatePatient(patientId);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
