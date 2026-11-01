package com.hospital.resourceallocation.security;

import com.hospital.resourceallocation.entity.Hospital;
import com.hospital.resourceallocation.entity.Role;
import com.hospital.resourceallocation.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class SecurityAndAuthTest {

    private JwtTokenProvider jwtTokenProvider;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                "SuperSecretKeyForCareGridTestingPurposesOnlyWhichIsVeryLong2026!",
                3600000L // 1 hour
        );
        passwordEncoder = new BCryptPasswordEncoder(12);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("BCrypt password hashing generates different hashes and verifies correctly")
    void testBCryptPasswordHashing() {
        String rawPassword = "Admin@CareGrid2026!";
        String encoded1 = passwordEncoder.encode(rawPassword);
        String encoded2 = passwordEncoder.encode(rawPassword);

        assertNotEquals(rawPassword, encoded1);
        assertNotEquals(encoded1, encoded2); // Salt differs each time
        assertTrue(passwordEncoder.matches(rawPassword, encoded1));
        assertTrue(passwordEncoder.matches(rawPassword, encoded2));
        assertFalse(passwordEncoder.matches("WrongPassword", encoded1));
    }

    @Test
    @DisplayName("JWT Token generation and validation works for valid user")
    void testJwtTokenGenerationAndValidation() {
        Hospital hospital = new Hospital();
        hospital.setName("Government General Hospital");

        User user = new User();
        user.setUsername("operator_ggh");
        user.setEmail("operator@caregrid.gov.in");
        user.setRole(Role.HOSPITAL_OPERATOR);
        user.setHospital(hospital);
        user.setActive(true);

        UserPrincipal principal = UserPrincipal.create(user);
        String token = jwtTokenProvider.generateToken(principal);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("operator_ggh", jwtTokenProvider.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("JWT Token validation rejects tampered token")
    void testJwtTokenRejectsTamperedToken() {
        Hospital hospital = new Hospital();
        hospital.setName("Government General Hospital");

        User user = new User();
        user.setUsername("operator_ggh");
        user.setEmail("operator@caregrid.gov.in");
        user.setRole(Role.HOSPITAL_OPERATOR);
        user.setHospital(hospital);

        UserPrincipal principal = UserPrincipal.create(user);
        String token = jwtTokenProvider.generateToken(principal);

        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";
        assertFalse(jwtTokenProvider.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("SecurityUtils permits admin to access any hospital")
    void testAdminAccessAllowedAnyHospital() {
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@caregrid.gov.in");
        admin.setRole(Role.ADMIN);
        admin.setActive(true);

        UserPrincipal principal = UserPrincipal.create(admin);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        assertTrue(SecurityUtils.isAdmin());
        assertFalse(SecurityUtils.isHospitalOperator());

        // Should not throw exception for any hospital ID
        assertDoesNotThrow(() -> SecurityUtils.validateHospitalAccess(1L));
        assertDoesNotThrow(() -> SecurityUtils.validateHospitalAccess(2L));
        assertDoesNotThrow(() -> SecurityUtils.validateHospitalAccess(999L));
    }

    @Test
    @DisplayName("SecurityUtils restricts operator to their assigned hospital only")
    void testOperatorRestrictedToOwnHospital() {
        Hospital hospital1 = new Hospital();
        hospital1.setName("Hospital 1");
        // Mock hospital ID
        User operator = new User();
        operator.setUsername("operator_h1");
        operator.setEmail("operator@h1.gov.in");
        operator.setRole(Role.HOSPITAL_OPERATOR);
        operator.setHospital(hospital1);
        operator.setActive(true);

        UserPrincipal principal = new UserPrincipal(
                10L, "operator_h1", "operator@h1.gov.in", "pass",
                Role.HOSPITAL_OPERATOR, 1L, "Hospital 1", true, principalAuthorities(Role.HOSPITAL_OPERATOR)
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        assertFalse(SecurityUtils.isAdmin());
        assertTrue(SecurityUtils.isHospitalOperator());
        assertEquals(1L, SecurityUtils.getCurrentHospitalId().orElse(null));

        // Permitted for assigned hospital (1L)
        assertDoesNotThrow(() -> SecurityUtils.validateHospitalAccess(1L));

        // Forbidden for another hospital (2L)
        assertThrows(AccessDeniedException.class, () -> SecurityUtils.validateHospitalAccess(2L));
    }

    private java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> principalAuthorities(Role role) {
        return java.util.Collections.singletonList(
                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
