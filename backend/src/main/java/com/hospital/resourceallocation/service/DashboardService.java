package com.hospital.resourceallocation.service;

import com.hospital.resourceallocation.entity.Bed;
import com.hospital.resourceallocation.entity.Doctor;
import com.hospital.resourceallocation.entity.Hospital;
import com.hospital.resourceallocation.entity.Patient;
import com.hospital.resourceallocation.repository.BedRepository;
import com.hospital.resourceallocation.repository.DoctorRepository;
import com.hospital.resourceallocation.repository.HospitalRepository;
import com.hospital.resourceallocation.repository.PatientRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class DashboardService {

    private final PatientRepository patientRepository;
    private final HospitalRepository hospitalRepository;
    private final BedRepository bedRepository;
    private final DoctorRepository doctorRepository;

    public DashboardService(
            PatientRepository patientRepository,
            HospitalRepository hospitalRepository,
            BedRepository bedRepository,
            DoctorRepository doctorRepository) {

        this.patientRepository = patientRepository;
        this.hospitalRepository = hospitalRepository;
        this.bedRepository = bedRepository;
        this.doctorRepository = doctorRepository;
    }

    public record HospitalResources(
            Long hospitalId,
            String hospitalName,
            String city,
            boolean active,
            long totalBeds,
            long availableBeds,
            long occupiedBeds,
            long totalIcuBeds,
            long availableIcuBeds,
            long occupiedIcuBeds,
            long totalGeneralBeds,
            long availableGeneralBeds,
            long occupiedGeneralBeds,
            long totalDoctors,
            long availableDoctors,
            long unavailableDoctors
    ) {}

    public record DashboardSummary(
            long totalHospitals,
            long activeHospitals,
            long totalPatients,
            long registeredPatients,
            long waitingPatients,
            long admittedPatients,
            long dischargedPatients,
            long totalBeds,
            long availableBeds,
            long occupiedBeds,
            long totalIcuBeds,
            long availableIcuBeds,
            long occupiedIcuBeds,
            long totalGeneralBeds,
            long availableGeneralBeds,
            long occupiedGeneralBeds,
            long totalDoctors,
            long availableDoctors,
            long unavailableDoctors,
            List<HospitalResources> hospitals,
            Long scopedHospitalId,
            String scopedHospitalName,
            boolean operatorView
    ) {
        // Backward-compatible constructor for system-wide dashboard
        public DashboardSummary(
                long totalHospitals,
                long activeHospitals,
                long totalPatients,
                long registeredPatients,
                long waitingPatients,
                long admittedPatients,
                long dischargedPatients,
                long totalBeds,
                long availableBeds,
                long occupiedBeds,
                long totalIcuBeds,
                long availableIcuBeds,
                long totalGeneralBeds,
                long availableGeneralBeds,
                long totalDoctors,
                long availableDoctors,
                long unavailableDoctors,
                List<HospitalResources> hospitals
        ) {
            this(
                    totalHospitals,
                    activeHospitals,
                    totalPatients,
                    registeredPatients,
                    waitingPatients,
                    admittedPatients,
                    dischargedPatients,
                    totalBeds,
                    availableBeds,
                    occupiedBeds,
                    totalIcuBeds,
                    availableIcuBeds,
                    totalIcuBeds - availableIcuBeds,
                    totalGeneralBeds,
                    availableGeneralBeds,
                    totalGeneralBeds - availableGeneralBeds,
                    totalDoctors,
                    availableDoctors,
                    unavailableDoctors,
                    hospitals,
                    null,
                    null,
                    false
            );
        }
    }

    @Transactional(readOnly = true)
    public DashboardSummary getSummary() {

        List<Hospital> hospitals = hospitalRepository.findAll();
        List<Patient> patients = patientRepository.findAll();

        long activeHospitals = hospitals.stream()
                .filter(Hospital::isActive)
                .count();

        long registeredPatients = countPatients(patients, "REGISTERED");
        long waitingPatients = countPatients(patients, "WAITING");
        long admittedPatients = countPatients(patients, "ADMITTED");
        long dischargedPatients = countPatients(patients, "DISCHARGED");

        List<HospitalResources> resources = hospitals.stream()
                .map(hospital -> getHospitalResources(hospital))
                .toList();

        long totalBeds = sum(resources, HospitalResources::totalBeds);
        long availableBeds = sum(resources, HospitalResources::availableBeds);
        long totalIcuBeds = sum(resources, HospitalResources::totalIcuBeds);
        long availableIcuBeds = sum(resources, HospitalResources::availableIcuBeds);
        long totalGeneralBeds = sum(resources, HospitalResources::totalGeneralBeds);
        long availableGeneralBeds = sum(resources, HospitalResources::availableGeneralBeds);
        long totalDoctors = sum(resources, HospitalResources::totalDoctors);
        long availableDoctors = sum(resources, HospitalResources::availableDoctors);
        long unavailableDoctors = sum(resources, HospitalResources::unavailableDoctors);

        return new DashboardSummary(
                hospitals.size(),
                activeHospitals,
                patients.size(),
                registeredPatients,
                waitingPatients,
                admittedPatients,
                dischargedPatients,
                totalBeds,
                availableBeds,
                totalBeds - availableBeds,
                totalIcuBeds,
                availableIcuBeds,
                totalGeneralBeds,
                availableGeneralBeds,
                totalDoctors,
                availableDoctors,
                unavailableDoctors,
                resources
        );
    }

    @Transactional(readOnly = true)
    public DashboardSummary getHospitalScopedSummary(Long hospitalId) {

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Hospital not found with ID: " + hospitalId
                ));

        HospitalResources resources = getHospitalResources(hospital);

        List<Patient> hospitalPatients = patientRepository.findAllByHospitalOrAssignedBedHospital(hospitalId);

        long registeredPatients = countPatients(hospitalPatients, "REGISTERED");
        long waitingPatients = countPatients(hospitalPatients, "WAITING");
        long admittedPatients = countPatients(hospitalPatients, "ADMITTED");
        long dischargedPatients = countPatients(hospitalPatients, "DISCHARGED");

        return new DashboardSummary(
                1,
                hospital.isActive() ? 1 : 0,
                hospitalPatients.size(),
                registeredPatients,
                waitingPatients,
                admittedPatients,
                dischargedPatients,
                resources.totalBeds(),
                resources.availableBeds(),
                resources.occupiedBeds(),
                resources.totalIcuBeds(),
                resources.availableIcuBeds(),
                resources.occupiedIcuBeds(),
                resources.totalGeneralBeds(),
                resources.availableGeneralBeds(),
                resources.occupiedGeneralBeds(),
                resources.totalDoctors(),
                resources.availableDoctors(),
                resources.unavailableDoctors(),
                List.of(resources),
                hospital.getId(),
                hospital.getName(),
                true
        );
    }

    @Transactional(readOnly = true)
    public HospitalResources getHospitalDashboard(Long hospitalId) {

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Hospital not found with ID: " + hospitalId
                ));

        return getHospitalResources(hospital);
    }

    private HospitalResources getHospitalResources(Hospital hospital) {

        List<Bed> beds = bedRepository.findByHospital_Id(hospital.getId());
        List<Doctor> doctors = doctorRepository.findByHospital_Id(hospital.getId());

        long totalIcuBeds = beds.stream()
                .filter(bed -> "ICU".equalsIgnoreCase(bed.getType()))
                .count();

        long availableIcuBeds = beds.stream()
                .filter(bed -> "ICU".equalsIgnoreCase(bed.getType()))
                .filter(Bed::isAvailable)
                .count();

        long totalGeneralBeds = beds.stream()
                .filter(bed -> "GENERAL".equalsIgnoreCase(bed.getType()))
                .count();

        long availableGeneralBeds = beds.stream()
                .filter(bed -> "GENERAL".equalsIgnoreCase(bed.getType()))
                .filter(Bed::isAvailable)
                .count();

        long availableBeds = beds.stream()
                .filter(Bed::isAvailable)
                .count();

        long availableDoctors = doctors.stream()
                .filter(Doctor::isAvailable)
                .count();

        return new HospitalResources(
                hospital.getId(),
                hospital.getName(),
                hospital.getCity(),
                hospital.isActive(),
                beds.size(),
                availableBeds,
                beds.size() - availableBeds,
                totalIcuBeds,
                availableIcuBeds,
                totalIcuBeds - availableIcuBeds,
                totalGeneralBeds,
                availableGeneralBeds,
                totalGeneralBeds - availableGeneralBeds,
                doctors.size(),
                availableDoctors,
                doctors.size() - availableDoctors
        );
    }

    private long countPatients(List<Patient> patients, String status) {
        return patients.stream()
                .filter(patient -> status.equalsIgnoreCase(patient.getStatus()))
                .count();
    }

    private long sum(
            List<HospitalResources> resources,
            java.util.function.ToLongFunction<HospitalResources> extractor) {

        return resources.stream()
                .mapToLong(extractor)
                .sum();
    }
}
