
package com.hospital.resourceallocation.repository;

import com.hospital.resourceallocation.entity.Hospital;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HospitalRepository
        extends JpaRepository<Hospital, Long> {

    @Override
    @EntityGraph(attributePaths = {"networkLocation"})
    List<Hospital> findAll();

    @Override
    @EntityGraph(attributePaths = {"networkLocation"})
    Optional<Hospital> findById(Long id);

    Optional<Hospital> findFirstByPrimaryHospitalTrue();

    List<Hospital> findByPrimaryHospitalFalseAndActiveTrue();

    List<Hospital> findByActiveTrue();
}
