package com.client.lms.dashboard.dto.response;

import java.time.Instant;

/**
 * One login event read from Keycloak (LOGIN, LOGIN_ERROR, LOGOUT, ...).
 */
public record LoginEventResponse(
        Instant time,
        String type,
        String keycloakUserId,
        String username,
        String ipAddress,
        String error
) {}
