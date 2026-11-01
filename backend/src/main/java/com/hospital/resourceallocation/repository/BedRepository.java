
package com.hospital.resourceallocation.repository;

import com.hospital.resourceallocation.entity.Bed;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BedRepository extends JpaRepository<Bed, Long> {

    List<Bed> findByHospital_Id(Long hospitalId);

    List<Bed> findByHospital_IdAndTypeAndAvailableTrue(
            Long hospitalId, String type);

    long countByHospital_IdAndTypeAndAvailableTrue(
            Long hospitalId, String type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Bed> findFirstByTypeAndAvailableTrueOrderByIdAsc(
            String type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b
        FROM Bed b
        WHERE UPPER(b.type) = UPPER(:type)
          AND b.available = true
        ORDER BY b.id
        """)
    List<Bed> findAvailableBedsForUpdate(
            @Param("type") String type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b
        FROM Bed b
        WHERE b.hospital.id = :hospitalId
          AND UPPER(b.type) = UPPER(:type)
          AND b.available = true
        ORDER BY b.id
        """)
    List<Bed> findAvailableBedsByHospitalForUpdate(
            @Param("hospitalId") Long hospitalId,
            @Param("type") String type);
}
