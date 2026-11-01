
package com.hospital.resourceallocation.repository;

import com.hospital.resourceallocation.entity.Doctor;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findByHospital_Id(Long hospitalId);

    List<Doctor> findByHospital_IdAndAvailableTrue(Long hospitalId);

    long countByHospital_IdAndAvailableTrue(Long hospitalId);

    List<Doctor> findByAssignedPatient_Id(Long patientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT d
        FROM Doctor d
        WHERE d.hospital.id = :hospitalId
          AND d.available = true
        ORDER BY d.id
        """)
    List<Doctor> findAvailableDoctorsForUpdate(
            @Param("hospitalId") Long hospitalId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT d
        FROM Doctor d
        WHERE d.hospital.id = :hospitalId
          AND d.available = true
          AND UPPER(d.specialization) LIKE UPPER(CONCAT('%', :specialization, '%'))
        ORDER BY d.id
        """)
    List<Doctor> findAvailableDoctorsBySpecializationForUpdate(
            @Param("hospitalId") Long hospitalId,
            @Param("specialization") String specialization);
}
