package com.client.lms.dashboard.service;

import java.util.Map;

import com.client.lms.dashboard.dto.response.DashboardStatsResponse;
import com.client.lms.dashboard.entity.Role;
import com.client.lms.dashboard.entity.UserStatus;
import com.client.lms.dashboard.integration.client.ClassroomServiceClient;
import com.client.lms.dashboard.integration.client.ProcessingStatsClient;
import com.client.lms.dashboard.integration.keycloak.KeycloakEventReader;
import com.client.lms.dashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case (Admin): see the system at a glance.
 * User counts come from this service. Classroom, file and processing counts come
 * from the owning services by REST. Login counts come from Keycloak events.
 * If another service is down, its block is returned empty instead of failing the page.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardStatsService {

    private final UserRepository users;
    private final ClassroomServiceClient classrooms;
    private final ProcessingStatsClient processing;
    private final KeycloakEventReader events;

    @Transactional(readOnly = true)
    public DashboardStatsResponse stats() {
        return new DashboardStatsResponse(userStats(), classroomStats(), fileStats(), processingStats(), loginStats());
    }

    private DashboardStatsResponse.Users userStats() {
        long admins = users.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE);
        long instructors = users.countByRoleAndStatus(Role.INSTRUCTOR, UserStatus.ACTIVE);
        long students = users.countByRoleAndStatus(Role.STUDENT, UserStatus.ACTIVE);
        long disabled = users.countByStatus(UserStatus.DISABLED);
        return new DashboardStatsResponse.Users(admins + instructors + students + disabled,
                admins, instructors, students, disabled);
    }

    @SuppressWarnings("unchecked")
    private DashboardStatsResponse.Classrooms classroomStats() {
        try {
            Map<String, Object> c = (Map<String, Object>) classrooms.stats().getOrDefault("classrooms", Map.of());
            return new DashboardStatsResponse.Classrooms(
                    num(c, "total"), num(c, "active"), num(c, "archived"), num(c, "courses"));
        } catch (Exception ex) {
            log.warn("classroom-service stats unavailable: {}", ex.getMessage());
            return new DashboardStatsResponse.Classrooms(0, 0, 0, 0);
        }
    }

    @SuppressWarnings("unchecked")
    private DashboardStatsResponse.Files fileStats() {
        try {
            Map<String, Object> f = (Map<String, Object>) classrooms.stats().getOrDefault("files", Map.of());
            Map<String, Long> byType = ((Map<String, Object>) f.getOrDefault("byType", Map.of()))
                    .entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey,
                            e -> ((Number) e.getValue()).longValue()));
            return new DashboardStatsResponse.Files(num(f, "total"), num(f, "totalSizeBytes"), byType);
        } catch (Exception ex) {
            log.warn("classroom-service file stats unavailable: {}", ex.getMessage());
            return new DashboardStatsResponse.Files(0, 0, Map.of());
        }
    }

    private DashboardStatsResponse.Processing processingStats() {
        try {
            return new DashboardStatsResponse.Processing(processing.jobsByStatus());
        } catch (Exception ex) {
            log.warn("content-processing-service stats unavailable: {}", ex.getMessage());
            return new DashboardStatsResponse.Processing(Map.of());
        }
    }

    private DashboardStatsResponse.Logins loginStats() {
        try {
            long ok = events.countByType("LOGIN", 7);
            long failed = events.countByType("LOGIN_ERROR", 7);
            return new DashboardStatsResponse.Logins(ok, failed, 0);
        } catch (Exception ex) {
            log.warn("Keycloak events unavailable: {}", ex.getMessage());
            return new DashboardStatsResponse.Logins(0, 0, 0);
        }
    }

    private static long num(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof Number n ? n.longValue() : 0;
    }
}
