package com.hospital.resourceallocation.service;

import com.hospital.resourceallocation.entity.*;
import com.hospital.resourceallocation.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReferralAndAllocationServiceTest {

    @Mock private PatientRepository patientRepository;
    @Mock private HospitalRepository hospitalRepository;
    @Mock private HospitalRouteRepository hospitalRouteRepository;
    @Mock private LocationRouteRepository locationRouteRepository;
    @Mock private NetworkLocationRepository networkLocationRepository;
    @Mock private BedRepository bedRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private WaitingQueueService waitingQueueService;

    private ReferralService referralService;
    private AllocationService allocationService;
    private DischargeService dischargeService;

    private Hospital primaryHospital;
    private Hospital referralB;
    private Hospital referralC;
    private Hospital referralD;

    private NetworkLocation loc1, loc2, loc3, loc4, loc5, loc6, loc7, loc8;

    @BeforeEach
    void setUp() {
        referralService = new ReferralService(
                patientRepository,
                hospitalRepository,
                hospitalRouteRepository,
                locationRouteRepository,
                networkLocationRepository,
                bedRepository,
                doctorRepository
        );

        allocationService = new AllocationService(
                patientRepository,
                bedRepository,
                doctorRepository,
                hospitalRepository,
                referralService
        );

        dischargeService = new DischargeService(
                patientRepository,
                bedRepository,
                doctorRepository,
                waitingQueueService
        );

        // Setup 8 locations
        loc1 = createLocation(1L, "Primary Hub", "Erode");
        loc2 = createLocation(2L, "District Hub", "Erode");
        loc3 = createLocation(3L, "City Hub", "Erode");
        loc4 = createLocation(4L, "Community Hub", "Erode");
        loc5 = createLocation(5L, "Central Junction", "Erode");
        loc6 = createLocation(6L, "North Bypass", "Erode");
        loc7 = createLocation(7L, "Ring Road Point", "Erode");
        loc8 = createLocation(8L, "East Link", "Erode");

        // Setup 4 hospitals (1 primary, 3 referral)
        primaryHospital = createHospital(1L, "Primary Hospital", true, loc1);
        referralB = createHospital(2L, "Referral Hospital B", false, loc2);
        referralC = createHospital(3L, "Referral Hospital C", false, loc3);
        referralD = createHospital(4L, "Referral Hospital D", false, loc4);
    }

    private NetworkLocation createLocation(Long id, String name, String city) {
        NetworkLocation loc = new NetworkLocation();
        try {
            var idField = NetworkLocation.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(loc, id);
        } catch (Exception ignored) {}
        loc.setName(name);
        loc.setCity(city);
        return loc;
    }

    private Hospital createHospital(Long id, String name, boolean isPrimary, NetworkLocation loc) {
        Hospital hosp = new Hospital();
        try {
            var idField = Hospital.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(hosp, id);
        } catch (Exception ignored) {}
        hosp.setName(name);
        hosp.setCity("Erode");
        hosp.setAddress("Test Address");
        hosp.setActive(true);
        hosp.setPrimaryHospital(isPrimary);
        hosp.setNetworkLocation(loc);
        return hosp;
    }

    private Bed createBed(Long id, String number, String type, boolean available, Hospital hosp) {
        Bed bed = new Bed();
        try {
            var idField = Bed.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(bed, id);
        } catch (Exception ignored) {}
        bed.setBedNumber(number);
        bed.setType(type);
        bed.setAvailable(available);
        bed.setHospital(hosp);
        return bed;
    }

    private Doctor createDoctor(Long id, String name, String spec, boolean available, Hospital hosp) {
        Doctor doc = new Doctor();
        try {
            var idField = Doctor.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(doc, id);
        } catch (Exception ignored) {}
        doc.setName(name);
        doc.setSpecialization(spec);
        doc.setAvailable(available);
        doc.setHospital(hosp);
        return doc;
    }

    private Patient createPatient(Long id, String name, String severity, String status) {
        Patient p = new Patient();
        try {
            var idField = Patient.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(p, id);
        } catch (Exception ignored) {}
        p.setPatientName(name);
        p.setSeverity(severity);
        p.setStatus(status);
        p.setArrivalTime(LocalDateTime.now());
        return p;
    }

    private LocationRoute createRoute(Long id, NetworkLocation src, NetworkLocation dst, int mins) {
        LocationRoute route = new LocationRoute();
        try {
            var idField = LocationRoute.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(route, id);
        } catch (Exception ignored) {}
        route.setSourceLocation(src);
        route.setDestinationLocation(dst);
        route.setTravelMinutes(mins);
        return route;
    }

    @Test
    @DisplayName("Primary hospital admits patient when compatible bed and doctor are available")
    void testPrimaryHospitalAdmitLocally() {
        Patient patient = createPatient(100L, "John Doe", "CRITICAL", "REGISTERED");
        when(patientRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(patient));
        when(hospitalRepository.findFirstByPrimaryHospitalTrue()).thenReturn(Optional.of(primaryHospital));

        Bed icuBed = createBed(10L, "ICU-01", "ICU", true, primaryHospital);
        when(bedRepository.findAvailableBedsByHospitalForUpdate(1L, "ICU")).thenReturn(List.of(icuBed));

        Doctor icuDoc = createDoctor(20L, "Dr. Senthil", "ICU", true, primaryHospital);
        when(doctorRepository.findAvailableDoctorsBySpecializationForUpdate(1L, "ICU")).thenReturn(List.of(icuDoc));

        var response = allocationService.allocatePatient(100L);

        assertEquals("ADMITTED", response.status());
        assertEquals("ICU-01", response.bedNumber());
        assertEquals("Dr. Senthil", response.doctorName());
        assertEquals(1L, response.hospitalId());
        assertFalse(icuBed.isAvailable());
        assertFalse(icuDoc.isAvailable());
        assertEquals(patient, icuDoc.getAssignedPatient());
    }

    @Test
    @DisplayName("When primary hospital cannot admit, finds fastest reachable referral hospital and reserves resources")
    void testReferralWhenPrimaryFull() {
        Patient patient = createPatient(101L, "Jane Doe", "CRITICAL", "REGISTERED");
        when(patientRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(patient));
        when(hospitalRepository.findFirstByPrimaryHospitalTrue()).thenReturn(Optional.of(primaryHospital));
        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(primaryHospital));

        // Primary hospital has no ICU beds available
        when(bedRepository.findAvailableBedsByHospitalForUpdate(1L, "ICU")).thenReturn(Collections.emptyList());

        // Setup network routes
        List<LocationRoute> networkRoutes = List.of(
                createRoute(1L, loc1, loc5, 5), createRoute(2L, loc5, loc1, 5),
                createRoute(3L, loc5, loc2, 8), createRoute(4L, loc2, loc5, 8), // Primary -> B = 13m
                createRoute(5L, loc1, loc6, 12), createRoute(6L, loc6, loc1, 12),
                createRoute(7L, loc6, loc4, 6), createRoute(8L, loc4, loc6, 6)   // Primary -> D = 18m
        );
        when(locationRouteRepository.findAllWithLocations()).thenReturn(networkRoutes);
        when(hospitalRepository.findByActiveTrue()).thenReturn(List.of(primaryHospital, referralB, referralD));

        // Referral Hospital B has ICU bed and doctor
        Bed bBed = createBed(201L, "B-ICU-01", "ICU", true, referralB);
        when(bedRepository.findByHospital_IdAndTypeAndAvailableTrue(2L, "ICU")).thenReturn(List.of(bBed));
        Doctor bDoc = createDoctor(202L, "Dr. B-ICU", "ICU", true, referralB);
        when(doctorRepository.findByHospital_IdAndAvailableTrue(2L)).thenReturn(List.of(bDoc));

        // Referral Hospital D has ICU bed and doctor
        Bed dBed = createBed(401L, "D-ICU-01", "ICU", true, referralD);
        when(bedRepository.findByHospital_IdAndTypeAndAvailableTrue(4L, "ICU")).thenReturn(List.of(dBed));
        Doctor dDoc = createDoctor(402L, "Dr. D-ICU", "ICU", true, referralD);
        when(doctorRepository.findByHospital_IdAndAvailableTrue(4L)).thenReturn(List.of(dDoc));

        when(networkLocationRepository.findAll()).thenReturn(List.of(loc1, loc2, loc4, loc5, loc6));

        var response = allocationService.allocatePatient(101L);

        // Hospital B is 13 mins away (faster than D at 18 mins)
        assertEquals("REFERRED", response.status());
        assertEquals(2L, response.hospitalId());
        assertEquals("B-ICU-01", response.bedNumber());
        assertEquals("Dr. B-ICU", response.doctorName());
        assertFalse(bBed.isAvailable());
        assertFalse(bDoc.isAvailable());
    }

    @Test
    @DisplayName("When no hospital has resources, patient remains in waiting queue")
    void testWaitingQueueWhenNoHospitalHasResources() {
        Patient patient = createPatient(102L, "Waiting Patient", "CRITICAL", "REGISTERED");
        when(patientRepository.findByIdForUpdate(102L)).thenReturn(Optional.of(patient));
        when(hospitalRepository.findFirstByPrimaryHospitalTrue()).thenReturn(Optional.of(primaryHospital));
        when(hospitalRepository.findById(1L)).thenReturn(Optional.of(primaryHospital));

        // Primary hospital full
        when(bedRepository.findAvailableBedsByHospitalForUpdate(1L, "ICU")).thenReturn(Collections.emptyList());

        // All referral hospitals full
        when(hospitalRepository.findByActiveTrue()).thenReturn(List.of(primaryHospital, referralB));
        when(bedRepository.findByHospital_IdAndTypeAndAvailableTrue(2L, "ICU")).thenReturn(Collections.emptyList());

        var response = allocationService.allocatePatient(102L);

        assertEquals("WAITING", response.status());
        assertNull(response.bedId());
        assertEquals("WAITING", patient.getStatus());
        verify(patientRepository).save(patient);
    }

    @Test
    @DisplayName("Explicit transfer completion changes status from REFERRED to ADMITTED")
    void testCompleteTransfer() {
        Patient patient = createPatient(103L, "Referred Patient", "SERIOUS", "REFERRED");
        patient.setAssignedBedId(50L);
        when(patientRepository.findByIdForUpdate(103L)).thenReturn(Optional.of(patient));

        Bed bed = createBed(50L, "GEN-201", "GENERAL", false, referralB);
        when(bedRepository.findById(50L)).thenReturn(Optional.of(bed));

        Doctor doc = createDoctor(60L, "Dr. Transfer", "GENERAL", false, referralB);
        when(doctorRepository.findByAssignedPatient_Id(103L)).thenReturn(List.of(doc));

        var response = referralService.completeTransfer(103L);

        assertEquals("ADMITTED", response.status());
        assertEquals("ADMITTED", patient.getStatus());
        verify(patientRepository).save(patient);
    }

    @Test
    @DisplayName("Discharge releases bed and doctor, and triggers waiting queue assignment")
    void testDischargeAndRelease() {
        Patient patient = createPatient(104L, "Discharging Patient", "GENERAL", "ADMITTED");
        patient.setAssignedBedId(88L);
        when(patientRepository.findByIdForUpdate(104L)).thenReturn(Optional.of(patient));

        Bed bed = createBed(88L, "GEN-88", "GENERAL", false, primaryHospital);
        when(bedRepository.findById(88L)).thenReturn(Optional.of(bed));

        Doctor doc = createDoctor(99L, "Dr. Kumar", "GENERAL", false, primaryHospital);
        when(doctorRepository.findByAssignedPatient_Id(104L)).thenReturn(List.of(doc));

        var response = dischargeService.dischargePatient(104L);

        assertEquals("DISCHARGED", patient.getStatus());
        assertTrue(bed.isAvailable());
        assertTrue(doc.isAvailable());
        assertNull(doc.getAssignedPatient());
        verify(waitingQueueService).assignNextPatient(bed);
    }
}
