package com.client.lms.dashboard.dto.response;

import java.util.Map;

/**
 * Counts shown on the Admin dashboard home page.
 * Counts-only for now (assumed). Charts over time can be added later.
 */
public record DashboardStatsResponse(
        Users users,
        Classrooms classrooms,
        Files files,
        Processing processing,
        Logins logins
) {
    public record Users(long total, long admins, long instructors, long students, long disabled) {}

    public record Classrooms(long total, long active, long archived, long courses) {}

    public record Files(long total, long totalSizeBytes, Map<String, Long> byType) {}

    /** processing_jobs grouped by status: QUEUED, RUNNING, DONE, FAILED. */
    public record Processing(Map<String, Long> byStatus) {}

    /** Login audit summary from Keycloak events for the last 7 days. */
    public record Logins(long success7d, long failed7d, long lockedAccounts) {}
}
