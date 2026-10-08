package com.client.lms.dashboard.dto.response;

import java.time.Instant;
import java.util.UUID;

/**
 * Classroom summary as returned by classroom-service (admin view).
 */
public record ClassroomSummaryResponse(
        UUID id,
        String name,
        String inviteCode,
        UUID instructorId,
        String instructorName,
        String status,
        long memberCount,
        long courseCount,
        Instant createdAt
) {}
