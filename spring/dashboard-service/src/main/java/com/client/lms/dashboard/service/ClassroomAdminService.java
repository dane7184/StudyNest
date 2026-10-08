package com.client.lms.dashboard.service;

import java.util.List;
import java.util.UUID;

import com.client.lms.common.api.PageResponse;
import com.client.lms.dashboard.dto.response.ClassroomSummaryResponse;
import com.client.lms.dashboard.entity.AuditAction;
import com.client.lms.dashboard.event.AuditEventPublisher;
import com.client.lms.dashboard.integration.client.ClassroomServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Use cases (Admin): list all classrooms, view one, archive one.
 * classroom-service owns the data; this is a thin pass-through with audit.
 */
@Service
@RequiredArgsConstructor
public class ClassroomAdminService {

    private final ClassroomServiceClient classrooms;
    private final AuditEventPublisher audit;

    public PageResponse<ClassroomSummaryResponse> list(String q, String status, int page, int size) {
        return classrooms.listClassrooms(q, status, page, size);
    }

    public ClassroomSummaryResponse get(UUID id) {
        return classrooms.getClassroom(id);
    }

    public void archive(UUID id) {
        classrooms.archiveClassroom(id);
        audit.publish(AuditAction.CLASSROOM_ARCHIVED, "CLASSROOM", id, null);
    }
}
