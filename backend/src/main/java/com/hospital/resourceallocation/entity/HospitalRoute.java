
package com.hospital.resourceallocation.entity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "hospital_routes",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"source_hospital_id", "destination_hospital_id"}
    )
)
public class HospitalRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_hospital_id", nullable = false)
    private Hospital sourceHospital;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_hospital_id", nullable = false)
    private Hospital destinationHospital;

    @Column(nullable = false)
    private int travelMinutes;

    public HospitalRoute() {
    }

    public Long getId() {
        return id;
    }

    public Hospital getSourceHospital() {
        return sourceHospital;
    }

    public void setSourceHospital(Hospital sourceHospital) {
        this.sourceHospital = sourceHospital;
    }

    public Hospital getDestinationHospital() {
        return destinationHospital;
    }

    public void setDestinationHospital(Hospital destinationHospital) {
        this.destinationHospital = destinationHospital;
    }

    public int getTravelMinutes() {
        return travelMinutes;
    }

    public void setTravelMinutes(int travelMinutes) {
        this.travelMinutes = travelMinutes;
    }
}
