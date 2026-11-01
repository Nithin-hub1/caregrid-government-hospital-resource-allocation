
package com.hospital.resourceallocation.repository;

import com.hospital.resourceallocation.entity.HospitalRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HospitalRouteRepository
        extends JpaRepository<HospitalRoute, Long> {

    @Query("""
        SELECT r
        FROM HospitalRoute r
        JOIN FETCH r.sourceHospital
        JOIN FETCH r.destinationHospital
        ORDER BY r.id
        """)
    List<HospitalRoute> findAllWithHospitals();
}
