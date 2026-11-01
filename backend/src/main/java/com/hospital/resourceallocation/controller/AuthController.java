package com.hospital.resourceallocation.controller;

import com.hospital.resourceallocation.entity.User;
import com.hospital.resourceallocation.repository.UserRepository;
import com.hospital.resourceallocation.security.JwtTokenProvider;
import com.hospital.resourceallocation.security.SecurityUtils;
import com.hospital.resourceallocation.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
    }

    public record LoginRequest(
            @NotBlank(message = "Username or email is required") String usernameOrEmail,
            @NotBlank(message = "Password is required") String password
    ) {}

    public record LoginResponse(
            String token,
            String tokenType,
            Long id,
            String username,
            String email,
            String role,
            String fullName,
            Long hospitalId,
            String hospitalName
    ) {}

    public record UserInfoResponse(
            Long id,
            String username,
            String email,
            String role,
            String fullName,
            Long hospitalId,
            String hospitalName
    ) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.usernameOrEmail().trim(),
                            request.password()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            String jwt = tokenProvider.generateToken(principal);

            User user = userRepository.findById(principal.getId()).orElse(null);
            String fullName = user != null ? user.getFullName() : principal.getUsername();

            return ResponseEntity.ok(new LoginResponse(
                    jwt,
                    "Bearer",
                    principal.getId(),
                    principal.getUsername(),
                    principal.getEmail(),
                    principal.getRole().name(),
                    fullName,
                    principal.getHospitalId(),
                    principal.getHospitalName()
            ));
        } catch (BadCredentialsException | UsernameNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username/email or password"));
        } catch (DisabledException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Your account has been deactivated. Please contact an administrator."));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Authentication error: " + ex.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        return SecurityUtils.getCurrentUserPrincipal()
                .map(principal -> {
                    User user = userRepository.findById(principal.getId()).orElse(null);
                    String fullName = user != null ? user.getFullName() : principal.getUsername();

                    return ResponseEntity.ok(new UserInfoResponse(
                            principal.getId(),
                            principal.getUsername(),
                            principal.getEmail(),
                            principal.getRole().name(),
                            fullName,
                            principal.getHospitalId(),
                            principal.getHospitalName()
                    ));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
