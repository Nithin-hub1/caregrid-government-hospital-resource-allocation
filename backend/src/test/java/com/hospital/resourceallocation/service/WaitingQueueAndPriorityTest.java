package com.hospital.resourceallocation.service;

import com.hospital.resourceallocation.entity.Bed;
import com.hospital.resourceallocation.entity.Doctor;
import com.hospital.resourceallocation.entity.Hospital;
import com.hospital.resourceallocation.entity.Patient;
import com.hospital.resourceallocation.repository.BedRepository;
import com.hospital.resourceallocation.repository.DoctorRepository;
import com.hospital.resourceallocation.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitingQueueAndPriorityTest {

    @Mock private PatientRepository patientRepository;
    @Mock private BedRepository bedRepository;
    @Mock private DoctorRepository doctorRepository;

    private WaitingQueueService waitingQueueService;
    private Hospital hospital;

    @BeforeEach
    void setUp() {
        waitingQueueService = new WaitingQueueService(patientRepository, bedRepository, doctorRepository);

        hospital = new Hospital();
        try {
            var idField = Hospital.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(hospital, 1L);
        } catch (Exception ignored) {}
        hospital.setName("General Hospital");
        hospital.setActive(true);
    }

    private Patient createPatient(Long id, String name, String severity, LocalDateTime arrival) {
        Patient p = new Patient();
        try {
            var idField = Patient.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(p, id);
        } catch (Exception ignored) {}
        p.setPatientName(name);
        p.setSeverity(severity);
        p.setStatus("WAITING");
        p.setArrivalTime(arrival);
        return p;
    }

    private Bed createBed(Long id, String number, String type) {
        Bed b = new Bed();
        try {
            var idField = Bed.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(b, id);
        } catch (Exception ignored) {}
        b.setBedNumber(number);
        b.setType(type);
        b.setAvailable(true);
        b.setHospital(hospital);
        return b;
    }

    private Doctor createDoctor(Long id, String name, String spec) {
        Doctor d = new Doctor();
        try {
            var idField = Doctor.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(d, id);
        } catch (Exception ignored) {}
        d.setName(name);
        d.setSpecialization(spec);
        d.setAvailable(true);
        d.setHospital(hospital);
        return d;
    }

    @Test
    @DisplayName("Waiting queue assigns highest priority patient first (CRITICAL over SERIOUS over MINOR, and earlier arrival)")
    void testWaitingQueuePriority() {
        LocalDateTime now = LocalDateTime.now();
        Patient minorEarly = createPatient(1L, "Minor Early", "MINOR", now.minusMinutes(30));
        Patient seriousEarly = createPatient(2L, "Serious Early", "SERIOUS", now.minusMinutes(20));
        Patient criticalLate = createPatient(3L, "Critical Late", "CRITICAL", now.minusMinutes(5));
        Patient seriousLate = createPatient(4L, "Serious Late", "SERIOUS", now.minusMinutes(10));

        // When a GENERAL bed becomes available
        Bed generalBed = createBed(10L, "GEN-01", "GENERAL");
        Doctor generalDoc = createDoctor(20L, "Dr. Gen", "GENERAL");

        when(patientRepository.findByStatus("WAITING"))
                .thenReturn(List.of(minorEarly, seriousEarly, criticalLate, seriousLate));
        when(doctorRepository.findAvailableDoctorsBySpecializationForUpdate(1L, "GENERAL"))
                .thenReturn(List.of(generalDoc));

        // The critical patient needs ICU so is skipped for GENERAL bed.
        // Between seriousEarly (arrived 20 mins ago) and seriousLate (arrived 10 mins ago),
        // seriousEarly should be assigned!
        var assignment = waitingQueueService.assignNextPatient(generalBed);

        assertNotNull(assignment);
        assertEquals(2L, assignment.patientId());
        assertEquals("Serious Early", assignment.patientName());
        assertEquals("GEN-01", assignment.bedNumber());
        assertEquals("Dr. Gen", assignment.doctorName());
        assertFalse(generalBed.isAvailable());
        assertFalse(generalDoc.isAvailable());
        assertEquals(seriousEarly, generalDoc.getAssignedPatient());
    }

    @Test
    @DisplayName("Waiting queue assigns ICU bed to CRITICAL patient")
    void testIcuBedAssignedToCriticalPatient() {
        LocalDateTime now = LocalDateTime.now();
        Patient serious = createPatient(1L, "Serious Patient", "SERIOUS", now.minusMinutes(20));
        Patient critical = createPatient(2L, "Critical Patient", "CRITICAL", now.minusMinutes(10));

        Bed icuBed = createBed(99L, "ICU-99", "ICU");
        Doctor icuDoc = createDoctor(88L, "Dr. Intensivist", "ICU");

        when(patientRepository.findByStatus("WAITING")).thenReturn(List.of(serious, critical));
        when(doctorRepository.findAvailableDoctorsBySpecializationForUpdate(1L, "ICU"))
                .thenReturn(List.of(icuDoc));

        var assignment = waitingQueueService.assignNextPatient(icuBed);

        assertNotNull(assignment);
        assertEquals(2L, assignment.patientId());
        assertEquals("Critical Patient", assignment.patientName());
        assertEquals("ICU-99", assignment.bedNumber());
        assertEquals("Dr. Intensivist", assignment.doctorName());
    }
}
