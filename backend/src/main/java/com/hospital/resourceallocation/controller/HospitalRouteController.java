
package com.hospital.resourceallocation.controller;

import com.hospital.resourceallocation.entity.Hospital;
import com.hospital.resourceallocation.entity.HospitalRoute;
import com.hospital.resourceallocation.repository.HospitalRepository;
import com.hospital.resourceallocation.repository.HospitalRouteRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hospital-routes")
public class HospitalRouteController {

    private final HospitalRepository hospitalRepository;
    private final HospitalRouteRepository routeRepository;

    public HospitalRouteController(
            HospitalRepository hospitalRepository,
            HospitalRouteRepository routeRepository) {
        this.hospitalRepository = hospitalRepository;
        this.routeRepository = routeRepository;
    }

    public record RouteRequest(
            @NotNull Long sourceHospitalId,
            @NotNull Long destinationHospitalId,
            @Min(1) int travelMinutes) {
    }

    public record RouteResponse(
            Long id,
            Long sourceHospitalId,
            String sourceHospitalName,
            Long destinationHospitalId,
            String destinationHospitalName,
            int travelMinutes) {
    }

    @PostMapping
    public ResponseEntity<?> createRoute(
            @Valid @RequestBody RouteRequest request) {

        if (request.sourceHospitalId()
                .equals(request.destinationHospitalId())) {
            return ResponseEntity.badRequest().body(
                    Map.of("error",
                            "Source and destination hospitals must differ."));
        }

        if (request.travelMinutes() < 1) {
            return ResponseEntity.badRequest().body(
                    Map.of("error",
                            "Travel time must be at least one minute."));
        }

        Hospital source = hospitalRepository
                .findById(request.sourceHospitalId())
                .orElse(null);

        Hospital destination = hospitalRepository
                .findById(request.destinationHospitalId())
                .orElse(null);

        if (source == null || destination == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error",
                            "Source or destination hospital was not found."));
        }

        HospitalRoute route = new HospitalRoute();
        route.setSourceHospital(source);
        route.setDestinationHospital(destination);
        route.setTravelMinutes(request.travelMinutes());

        HospitalRoute saved = routeRepository.save(route);

        // Build the response while the hospital entities are available.
        RouteResponse response = toResponse(saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<RouteResponse> getRoutes() {

        return routeRepository.findAllWithHospitals()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private RouteResponse toResponse(HospitalRoute route) {
        return new RouteResponse(
                route.getId(),
                route.getSourceHospital().getId(),
                route.getSourceHospital().getName(),
                route.getDestinationHospital().getId(),
                route.getDestinationHospital().getName(),
                route.getTravelMinutes()
        );
    }
}
