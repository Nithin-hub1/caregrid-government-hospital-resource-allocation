package com.hospital.resourceallocation.repository;

import com.hospital.resourceallocation.entity.LocationRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationRouteRepository
        extends JpaRepository<LocationRoute, Long> {

    List<LocationRoute> findBySourceLocation_Id(Long sourceLocationId);

    List<LocationRoute> findByDestinationLocation_Id(Long destinationLocationId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT r
        FROM LocationRoute r
        JOIN FETCH r.sourceLocation
        JOIN FETCH r.destinationLocation
        ORDER BY r.id
        """)
    List<LocationRoute> findAllWithLocations();
}