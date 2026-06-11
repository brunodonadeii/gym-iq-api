package com.gymiq.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class PersonalDataExposurePolicy {

    private PersonalDataExposurePolicy() {
    }

    public static boolean canViewFullStudentData() {
        return hasAnyRole("ADMIN", "RECEPTION", "STUDENT");
    }

    public static boolean canViewFullInstructorData() {
        return hasAnyRole("ADMIN", "RECEPTION", "INSTRUCTOR");
    }

    public static boolean canViewFullAdministrativeUserData() {
        return hasAnyRole("ADMIN");
    }

    private static boolean hasAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        for (String role : roles) {
            String authority = "ROLE_" + role;
            boolean hasRole = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> authority.equals(grantedAuthority.getAuthority()));
            if (hasRole) {
                return true;
            }
        }
        return false;
    }
}
