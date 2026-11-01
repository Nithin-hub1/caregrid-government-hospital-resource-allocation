package com.hospital.resourceallocation.repository;

import com.hospital.resourceallocation.entity.NetworkLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NetworkLocationRepository
        extends JpaRepository<NetworkLocation, Long> {

    Optional<NetworkLocation> findByName(String name);
}