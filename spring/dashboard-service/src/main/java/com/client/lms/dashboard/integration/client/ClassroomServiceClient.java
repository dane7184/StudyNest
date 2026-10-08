package com.client.lms.dashboard.integration.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.client.lms.common.api.PageResponse;
import com.client.lms.dashboard.dto.response.ClassroomSummaryResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls classroom-service, which owns classrooms, classroom_members, courses and files.
 * The admin JWT is forwarded automatically.
 */
@Component
public class ClassroomServiceClient {

    private final RestClient client;

    public ClassroomServiceClient(RestClient classroomRestClient) {
        this.client = classroomRestClient;
    }

    /** GET /internal/admin/stats -> {classrooms:{total,active,archived,courses}, files:{total,totalSizeBytes,byType}} */
    public Map<String, Object> stats() {
        return client.get()
                .uri("/internal/admin/stats")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public PageResponse<ClassroomSummaryResponse> listClassrooms(String q, String status, int page, int size) {
        return client.get()
                .uri(b -> b.path("/internal/admin/classrooms")
                        .queryParam("q", q)
                        .queryParam("status", status)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public ClassroomSummaryResponse getClassroom(UUID classroomId) {
        return client.get()
                .uri("/internal/admin/classrooms/{id}", classroomId)
                .retrieve()
                .body(ClassroomSummaryResponse.class);
    }

    public void archiveClassroom(UUID classroomId) {
        client.post()
                .uri("/internal/admin/classrooms/{id}/archive", classroomId)
                .retrieve()
                .toBodilessEntity();
    }

    /** Enrols a user into a classroom by invite code (used by Excel import). */
    public void enrolByCode(UUID userId, String classroomCode) {
        client.post()
                .uri("/internal/admin/classrooms/enrol")
                .body(Map.of("userId", userId, "classroomCode", classroomCode))
                .retrieve()
                .toBodilessEntity();
    }

    /** Classrooms of one user, for the admin user detail page. */
    public List<ClassroomSummaryResponse> classroomsOfUser(UUID userId) {
        return client.get()
                .uri("/internal/admin/users/{id}/classrooms", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
