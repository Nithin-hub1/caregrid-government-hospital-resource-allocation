package com.hospital.resourceallocation.repository;

import com.hospital.resourceallocation.entity.Patient;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    List<Patient> findByStatus(String status);

    List<Patient> findByHospital_Id(Long hospitalId);

    List<Patient> findByHospital_IdAndStatus(Long hospitalId, String status);

    @Query("SELECT p FROM Patient p WHERE p.hospital.id = :hospitalId OR (p.assignedBedId IS NOT NULL AND p.assignedBedId IN (SELECT b.id FROM Bed b WHERE b.hospital.id = :hospitalId))")
    List<Patient> findAllByHospitalOrAssignedBedHospital(@Param("hospitalId") Long hospitalId);

    @Query("SELECT p FROM Patient p WHERE p.status = 'WAITING' AND (p.hospital.id = :hospitalId OR p.hospital IS NULL)")
    List<Patient> findWaitingQueueByHospital(@Param("hospitalId") Long hospitalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Patient p WHERE p.id = :patientId")
    Optional<Patient> findByIdForUpdate(@Param("patientId") Long patientId);
}
