
package com.hospital.resourceallocation.controller;

import com.hospital.resourceallocation.service.DischargeService;
import com.hospital.resourceallocation.service.DischargeService.DischargeResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/discharges")
public class DischargeController {

    private final DischargeService dischargeService;

    public DischargeController(DischargeService dischargeService) {
        this.dischargeService = dischargeService;
    }

    @PostMapping("/patients/{patientId}")
    public ResponseEntity<DischargeResponse> dischargePatient(
            @PathVariable Long patientId) {

        DischargeResponse response =
                dischargeService.dischargePatient(patientId);

        return ResponseEntity.ok(response);
    }
}
