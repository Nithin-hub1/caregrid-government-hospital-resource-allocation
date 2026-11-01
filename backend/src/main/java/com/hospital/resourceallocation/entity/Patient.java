package com.hospital.resourceallocation.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Patient name is required")
    @Column(nullable = false)
    private String patientName;

    @Min(value = 0, message = "Age cannot be negative")
    private int age;

    private String gender;

    @NotBlank(message = "Severity is required")
    @Column(nullable = false)
    private String severity;

    @Column(nullable = false)
    private String status = "REGISTERED";

    @Column(name = "assigned_bed_id")
    private Long assignedBedId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hospital_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Hospital hospital;

    // Records when the patient entered the system.
    @Column(name = "arrival_time")
    private LocalDateTime arrivalTime;

    public Patient() {
    }

    @PrePersist
    protected void setArrivalTimeBeforeInsert() {
        if (arrivalTime == null) {
            arrivalTime = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAssignedBedId() {
        return assignedBedId;
    }

    public void setAssignedBedId(Long assignedBedId) {
        this.assignedBedId = assignedBedId;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public void setHospital(Hospital hospital) {
        this.hospital = hospital;
    }

    public LocalDateTime getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(LocalDateTime arrivalTime) {
        this.arrivalTime = arrivalTime;
    }
}
