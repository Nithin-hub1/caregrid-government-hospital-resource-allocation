package com.hospital.resourceallocation.security;

import com.hospital.resourceallocation.entity.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UserPrincipal> getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static Optional<String> getCurrentUsername() {
        return getCurrentUserPrincipal().map(UserPrincipal::getUsername);
    }

    public static Optional<Role> getCurrentUserRole() {
        return getCurrentUserPrincipal().map(UserPrincipal::getRole);
    }

    public static Optional<Long> getCurrentHospitalId() {
        return getCurrentUserPrincipal().map(UserPrincipal::getHospitalId);
    }

    public static boolean isAdmin() {
        return getCurrentUserRole().map(role -> role == Role.ADMIN).orElse(false);
    }

    public static boolean isHospitalOperator() {
        return getCurrentUserRole().map(role -> role == Role.HOSPITAL_OPERATOR).orElse(false);
    }

    public static void validateHospitalAccess(Long targetHospitalId) {
        Optional<UserPrincipal> principalOpt = getCurrentUserPrincipal();
        if (principalOpt.isEmpty()) {
            // No security context (e.g. unit tests without authentication context)
            return;
        }

        UserPrincipal principal = principalOpt.get();
        if (principal.getRole() == Role.ADMIN) {
            return;
        }

        if (principal.getRole() == Role.HOSPITAL_OPERATOR) {
            Long userHospitalId = principal.getHospitalId();
            if (userHospitalId == null || !userHospitalId.equals(targetHospitalId)) {
                throw new AccessDeniedException(
                        "Access denied: Hospital Operator is only authorized for hospital ID "
                                + userHospitalId + " (requested: " + targetHospitalId + ")");
            }
            return;
        }

        throw new AccessDeniedException("Unauthorized role for this operation");
    }
}
