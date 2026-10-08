package com.client.lms.dashboard.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.client.lms.dashboard.entity.Role;
import com.client.lms.dashboard.entity.UserStatus;

public record UserResponse(
        UUID id,
        String keycloakUserId,
        String email,
        String fullName,
        String studentCode,
        Role role,
        UserStatus status,
        String preferredLanguage,
        /** Read from Keycloak on detail view: temporarily locked by brute-force protection. */
        Boolean locked,
        /** Read from Keycloak on detail view: has OTP configured. */
        Boolean mfaEnabled,
        Instant createdAt,
        Instant updatedAt
) {}
