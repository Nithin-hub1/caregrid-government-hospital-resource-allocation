package com.hospital.resourceallocation.controller;

import com.hospital.resourceallocation.entity.Hospital;
import com.hospital.resourceallocation.entity.LocationRoute;
import com.hospital.resourceallocation.entity.NetworkLocation;
import com.hospital.resourceallocation.repository.HospitalRepository;
import com.hospital.resourceallocation.repository.LocationRouteRepository;
import com.hospital.resourceallocation.repository.NetworkLocationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/network-locations")
@CrossOrigin(origins = "*")
public class NetworkLocationController {

    private final NetworkLocationRepository repository;
    private final LocationRouteRepository locationRouteRepository;
    private final HospitalRepository hospitalRepository;

    public NetworkLocationController(
            NetworkLocationRepository repository,
            LocationRouteRepository locationRouteRepository,
            HospitalRepository hospitalRepository) {
        this.repository = repository;
        this.locationRouteRepository = locationRouteRepository;
        this.hospitalRepository = hospitalRepository;
    }

    public record LocationRouteResponse(
            Long id,
            Long sourceLocationId,
            String sourceLocationName,
            Long destinationLocationId,
            String destinationLocationName,
            int travelMinutes
    ) {}

    public record NetworkOverviewResponse(
            int totalLocations,
            int totalRoutes,
            List<NetworkLocation> locations,
            List<LocationRouteResponse> routes,
            List<Hospital> hospitals
    ) {}

    @GetMapping
    public List<NetworkLocation> getAllLocations() {
        return repository.findAll();
    }

    @GetMapping("/routes")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<LocationRouteResponse> getAllRoutes() {
        return locationRouteRepository.findAllWithLocations().stream()
                .map(r -> new LocationRouteResponse(
                        r.getId(),
                        r.getSourceLocation().getId(),
                        r.getSourceLocation().getName(),
                        r.getDestinationLocation().getId(),
                        r.getDestinationLocation().getName(),
                        r.getTravelMinutes()))
                .toList();
    }

    @GetMapping("/overview")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public NetworkOverviewResponse getNetworkOverview() {
        List<NetworkLocation> locations = repository.findAll();
        List<LocationRouteResponse> routes = getAllRoutes();
        List<Hospital> hospitals = hospitalRepository.findAll();

        return new NetworkOverviewResponse(
                locations.size(),
                routes.size(),
                locations,
                routes,
                hospitals
        );
    }

    @GetMapping("/{id}")
    public NetworkLocation getLocationById(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Location not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NetworkLocation createLocation(
            @RequestBody NetworkLocation location) {

        if (location.getName() == null ||
                location.getName().isBlank() ||
                location.getCity() == null ||
                location.getCity().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Location name and city are required");
        }

        String name = location.getName().trim();

        if (repository.findByName(name).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A location with this name already exists");
        }

        location.setName(name);
        location.setCity(location.getCity().trim());

        return repository.save(location);
    }
}
