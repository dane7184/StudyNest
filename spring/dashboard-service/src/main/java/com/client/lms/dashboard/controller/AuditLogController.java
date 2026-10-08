package com.client.lms.dashboard.controller;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.client.lms.common.api.PageResponse;
import com.client.lms.dashboard.dto.response.AuditLogResponse;
import com.client.lms.dashboard.dto.response.LoginEventResponse;
import com.client.lms.dashboard.entity.AuditAction;
import com.client.lms.dashboard.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Audit log", description = "Admin actions (local) and login events (Keycloak)")
@RestController
@RequestMapping("/api/admin/audit-log")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService service;

    @Operation(summary = "Search admin actions")
    @GetMapping("/actions")
    public PageResponse<AuditLogResponse> actions(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) String actorKeycloakId,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.searchAdminActions(action, actorKeycloakId, targetId, from, to, pageable);
    }

    @Operation(summary = "Login / logout / password events from Keycloak")
    @GetMapping("/logins")
    public List<LoginEventResponse> logins(
            @RequestParam(required = false) String keycloakUserId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return service.loginEvents(keycloakUserId, from, to, page, size);
    }
}
