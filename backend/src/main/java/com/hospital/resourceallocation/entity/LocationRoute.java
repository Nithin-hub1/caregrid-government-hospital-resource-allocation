
package com.hospital.resourceallocation.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(
    name = "location_routes",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"source_location_id", "destination_location_id"}
    )
)
public class LocationRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_location_id", nullable = false)
    private NetworkLocation sourceLocation;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_location_id", nullable = false)
    private NetworkLocation destinationLocation;

    @Min(1)
    @Column(nullable = false)
    private int travelMinutes;

    public LocationRoute() {
    }

    public Long getId() {
        return id;
    }

    public NetworkLocation getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(NetworkLocation sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public NetworkLocation getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(NetworkLocation destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public int getTravelMinutes() {
        return travelMinutes;
    }

    public void setTravelMinutes(int travelMinutes) {
        this.travelMinutes = travelMinutes;
    }
}
