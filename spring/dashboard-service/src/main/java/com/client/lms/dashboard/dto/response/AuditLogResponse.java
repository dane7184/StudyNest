package com.client.lms.dashboard.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.client.lms.dashboard.entity.AuditAction;

public record AuditLogResponse(
        UUID id,
        AuditAction action,
        String actorKeycloakId,
        String actorEmail,
        String targetType,
        String targetId,
        String details,
        String ipAddress,
        Instant createdAt
) {}
