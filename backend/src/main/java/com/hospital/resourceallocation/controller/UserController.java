package com.hospital.resourceallocation.controller;

import com.hospital.resourceallocation.entity.Hospital;
import com.hospital.resourceallocation.entity.Role;
import com.hospital.resourceallocation.entity.User;
import com.hospital.resourceallocation.repository.HospitalRepository;
import com.hospital.resourceallocation.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(
            UserRepository userRepository,
            HospitalRepository hospitalRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.hospitalRepository = hospitalRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record CreateUserRequest(
            @NotBlank(message = "Username is required") String username,
            @NotBlank(message = "Email is required") @Email(message = "Invalid email format") String email,
            @NotBlank(message = "Password is required") String password,
            @NotNull(message = "Role is required") Role role,
            Long hospitalId,
            String fullName
    ) {}

    public record UpdateUserRequest(
            String email,
            String password,
            Role role,
            Long hospitalId,
            String fullName,
            Boolean active
    ) {}

    public record UserDto(
            Long id,
            String username,
            String email,
            Role role,
            Long hospitalId,
            String hospitalName,
            String fullName,
            boolean active,
            LocalDateTime createdAt
    ) {}

    @GetMapping
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(u -> ResponseEntity.ok(toDto(u)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Username is already taken"));
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email is already registered"));
        }

        Hospital hospital = null;
        if (request.role() == Role.HOSPITAL_OPERATOR) {
            if (request.hospitalId() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Hospital association is required for Hospital Operators"));
            }
            hospital = hospitalRepository.findById(request.hospitalId()).orElse(null);
            if (hospital == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Designated hospital not found with ID: " + request.hospitalId()));
            }
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setHospital(hospital);
        user.setFullName(request.fullName() != null ? request.fullName().trim() : username);
        user.setActive(true);

        User saved = userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request) {

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        if (request.email() != null && !request.email().isBlank()) {
            String newEmail = request.email().trim().toLowerCase();
            if (!newEmail.equalsIgnoreCase(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Email is already in use by another account"));
            }
            user.setEmail(newEmail);
        }

        if (request.password() != null && !request.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.password()));
        }

        if (request.role() != null) {
            user.setRole(request.role());
        }

        if (request.role() == Role.HOSPITAL_OPERATOR || (request.role() == null && user.getRole() == Role.HOSPITAL_OPERATOR)) {
            if (request.hospitalId() != null) {
                Hospital hospital = hospitalRepository.findById(request.hospitalId()).orElse(null);
                if (hospital == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body(Map.of("error", "Hospital not found with ID: " + request.hospitalId()));
                }
                user.setHospital(hospital);
            }
        } else if (user.getRole() == Role.ADMIN) {
            user.setHospital(null);
        }

        if (request.fullName() != null) {
            user.setFullName(request.fullName().trim());
        }

        if (request.active() != null) {
            user.setActive(request.active());
        }

        User updated = userRepository.save(user);
        return ResponseEntity.ok(toDto(updated));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> toggleUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }

        Boolean active = body.get("active");
        if (active == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "active boolean property is required"));
        }

        user.setActive(active);
        userRepository.save(user);

        return ResponseEntity.ok(toDto(user));
    }

    private UserDto toDto(User user) {
        Long hospitalId = user.getHospital() != null ? user.getHospital().getId() : null;
        String hospitalName = user.getHospital() != null ? user.getHospital().getName() : null;

        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                hospitalId,
                hospitalName,
                user.getFullName(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
