package com.hospital.resourceallocation.controller;

import com.hospital.resourceallocation.security.SecurityUtils;
import com.hospital.resourceallocation.service.ReferralService;
import com.hospital.resourceallocation.service.ReferralService.ReferralResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/referrals")
public class ReferralController {

    private final ReferralService referralService;

    public ReferralController(ReferralService referralService) {
        this.referralService = referralService;
    }

    public record ConfirmReferralRequest(
            Long patientId,
            Long sourceHospitalId,
            Long destinationHospitalId,
            Long bedId,
            Long doctorId
    ) {}

    @PostMapping("/patients/{patientId}")
    public ResponseEntity<ReferralResponse> recommendReferral(
            @PathVariable Long patientId,
            @RequestParam Long sourceHospitalId) {

        // Operators can only recommend referrals originating from their own hospital
        SecurityUtils.validateHospitalAccess(sourceHospitalId);

        ReferralResponse response =
                referralService.recommendReferral(
                        patientId, sourceHospitalId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm")
    public ResponseEntity<ReferralResponse> confirmReferral(
            @RequestBody ConfirmReferralRequest request) {

        if (request == null
                || request.patientId() == null
                || request.sourceHospitalId() == null
                || request.destinationHospitalId() == null
                || request.bedId() == null
                || request.doctorId() == null) {
            return ResponseEntity.badRequest().build();
        }

        // Operators can only confirm referrals originating from their own hospital
        SecurityUtils.validateHospitalAccess(request.sourceHospitalId());

        ReferralResponse response = referralService.confirmReferral(
                request.patientId(),
                request.sourceHospitalId(),
                request.destinationHospitalId(),
                request.bedId(),
                request.doctorId()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/patients/{patientId}/complete-transfer")
    public ResponseEntity<ReferralResponse> completeTransfer(
            @PathVariable Long patientId) {

        ReferralResponse response = referralService.completeTransfer(patientId);
        return ResponseEntity.ok(response);
    }
}
