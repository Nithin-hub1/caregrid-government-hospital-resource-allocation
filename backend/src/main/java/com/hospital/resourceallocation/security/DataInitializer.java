package com.hospital.resourceallocation.security;

import com.hospital.resourceallocation.entity.Hospital;
import com.hospital.resourceallocation.entity.Patient;
import com.hospital.resourceallocation.entity.Role;
import com.hospital.resourceallocation.entity.User;
import com.hospital.resourceallocation.repository.BedRepository;
import com.hospital.resourceallocation.repository.HospitalRepository;
import com.hospital.resourceallocation.repository.PatientRepository;
import com.hospital.resourceallocation.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final BedRepository bedRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${CAREGRID_ADMIN_PASSWORD:}")
    private String envAdminPassword;

    @Value("${CAREGRID_OPERATOR_PASSWORD:}")
    private String envOperatorPassword;

    public DataInitializer(
            UserRepository userRepository,
            HospitalRepository hospitalRepository,
            BedRepository bedRepository,
            PatientRepository patientRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.hospitalRepository = hospitalRepository;
        this.bedRepository = bedRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        initializeAdminUser();
        initializeOperatorUsers();
        migrateExistingPatientsHospital();
    }

    private void initializeAdminUser() {
        if (!userRepository.existsByRole(Role.ADMIN)) {
            String adminPassword = (envAdminPassword != null && !envAdminPassword.isBlank())
                    ? envAdminPassword.trim()
                    : "Admin@CareGrid2026!";

            if (envAdminPassword == null || envAdminPassword.isBlank()) {
                log.warn("[CareGrid Security Notice] CAREGRID_ADMIN_PASSWORD environment variable is not set. "
                        + "Using default initial bootstrap password for 'admin'. In production environments, set CAREGRID_ADMIN_PASSWORD.");
            }

            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@caregrid.gov.in");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setFullName("CareGrid System Administrator");
            admin.setActive(true);
            admin.setHospital(null);

            userRepository.save(admin);
            log.info("Initialized default Administrator account (username: 'admin')");
        }
    }

    private void initializeOperatorUsers() {
        if (userRepository.findByRole(Role.HOSPITAL_OPERATOR).isEmpty()) {
            List<Hospital> hospitals = hospitalRepository.findAll();
            if (hospitals.isEmpty()) {
                return;
            }

            String operatorPassword = (envOperatorPassword != null && !envOperatorPassword.isBlank())
                    ? envOperatorPassword.trim()
                    : "Operator@CareGrid2026!";

            // Hospital 1 operator
            Hospital hospital1 = hospitals.get(0);
            User operator1 = new User();
            operator1.setUsername("operator_ggh");
            operator1.setEmail("operator.ggh@caregrid.gov.in");
            operator1.setPassword(passwordEncoder.encode(operatorPassword));
            operator1.setRole(Role.HOSPITAL_OPERATOR);
            operator1.setFullName(hospital1.getName() + " Operator");
            operator1.setHospital(hospital1);
            operator1.setActive(true);
            userRepository.save(operator1);
            log.info("Initialized operator account 'operator_ggh' for hospital: {}", hospital1.getName());

            // Hospital 2 operator if available
            if (hospitals.size() > 1) {
                Hospital hospital2 = hospitals.get(1);
                User operator2 = new User();
                operator2.setUsername("operator_gdh");
                operator2.setEmail("operator.gdh@caregrid.gov.in");
                operator2.setPassword(passwordEncoder.encode(operatorPassword));
                operator2.setRole(Role.HOSPITAL_OPERATOR);
                operator2.setFullName(hospital2.getName() + " Operator");
                operator2.setHospital(hospital2);
                operator2.setActive(true);
                userRepository.save(operator2);
                log.info("Initialized operator account 'operator_gdh' for hospital: {}", hospital2.getName());
            }
        }
    }

    private void migrateExistingPatientsHospital() {
        List<Hospital> hospitals = hospitalRepository.findAll();
        if (hospitals.isEmpty()) return;
        Hospital primaryHospital = hospitalRepository.findFirstByPrimaryHospitalTrue()
                .orElse(hospitals.get(0));

        List<Patient> patients = patientRepository.findAll();
        for (Patient p : patients) {
            if (p.getHospital() == null) {
                if (p.getAssignedBedId() != null) {
                    bedRepository.findById(p.getAssignedBedId()).ifPresent(bed -> {
                        p.setHospital(bed.getHospital());
                        patientRepository.save(p);
                    });
                } else {
                    p.setHospital(primaryHospital);
                    patientRepository.save(p);
                }
            }
        }
    }
}
