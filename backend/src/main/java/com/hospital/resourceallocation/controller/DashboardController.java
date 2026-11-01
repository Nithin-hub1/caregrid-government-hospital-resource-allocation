package com.hospital.resourceallocation.controller;

import com.hospital.resourceallocation.security.SecurityUtils;
import com.hospital.resourceallocation.service.DashboardService;
import com.hospital.resourceallocation.service.DashboardService.DashboardSummary;
import com.hospital.resourceallocation.service.DashboardService.HospitalResources;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummary getSummary() {
        if (SecurityUtils.isHospitalOperator()) {
            Long hospitalId = SecurityUtils.getCurrentHospitalId()
                    .orElseThrow(() -> new AccessDeniedException("No hospital assigned to operator account"));
            return dashboardService.getHospitalScopedSummary(hospitalId);
        }
        return dashboardService.getSummary();
    }

    @GetMapping("/hospitals/{hospitalId}")
    public HospitalResources getHospitalDashboard(
            @PathVariable Long hospitalId) {

        SecurityUtils.validateHospitalAccess(hospitalId);
        return dashboardService.getHospitalDashboard(hospitalId);
    }
}
