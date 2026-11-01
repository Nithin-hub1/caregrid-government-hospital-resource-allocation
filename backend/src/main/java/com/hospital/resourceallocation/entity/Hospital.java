
package com.hospital.resourceallocation.entity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "hospitals")
public class Hospital {
@ManyToOne(fetch = FetchType.EAGER)
@JoinColumn(name = "network_location_id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
private NetworkLocation networkLocation;

public NetworkLocation getNetworkLocation() {
    return networkLocation;
}

public void setNetworkLocation(NetworkLocation networkLocation) {
    this.networkLocation = networkLocation;
}
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Hospital name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Hospital address is required")
    @Column(nullable = false)
    private String address;

    private String city;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "primary_hospital", nullable = false)
    private boolean primaryHospital = false;

    public Hospital() {
    }

    public Long getId() {
        return id;
    }

    public boolean isPrimaryHospital() {
        return primaryHospital;
    }

    public void setPrimaryHospital(boolean primaryHospital) {
        this.primaryHospital = primaryHospital;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
