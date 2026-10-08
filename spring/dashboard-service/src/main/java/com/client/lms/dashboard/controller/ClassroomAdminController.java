package com.client.lms.dashboard.controller;

import java.util.UUID;

import com.client.lms.common.api.PageResponse;
import com.client.lms.dashboard.dto.response.ClassroomSummaryResponse;
import com.client.lms.dashboard.service.ClassroomAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Classrooms (admin)", description = "View all classrooms; data lives in classroom-service")
@RestController
@RequestMapping("/api/admin/classrooms")
@RequiredArgsConstructor
public class ClassroomAdminController {

    private final ClassroomAdminService service;

    @GetMapping
    public PageResponse<ClassroomSummaryResponse> list(@RequestParam(required = false) String q,
                                                       @RequestParam(required = false) String status,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return service.list(q, status, page, size);
    }

    @GetMapping("/{id}")
    public ClassroomSummaryResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @Operation(summary = "Archive a classroom (read-only for members afterwards)")
    @PostMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@PathVariable UUID id) {
        service.archive(id);
        return ResponseEntity.noContent().build();
    }
}
