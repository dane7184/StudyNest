package com.client.lms.dashboard.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.client.lms.common.api.PageResponse;
import com.client.lms.dashboard.dto.response.AuditLogResponse;
import com.client.lms.dashboard.dto.response.LoginEventResponse;
import com.client.lms.dashboard.entity.AuditAction;
import com.client.lms.dashboard.entity.User;
import com.client.lms.dashboard.integration.keycloak.KeycloakEventReader;
import com.client.lms.dashboard.mapper.DashboardMapper;
import com.client.lms.dashboard.repository.AuditLogRepository;
import com.client.lms.dashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case (Admin): search the audit log.
 * Two sources: admin actions (local audit_log table) and login events (Keycloak).
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogs;
    private final UserRepository users;
    private final KeycloakEventReader events;
    private final DashboardMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> searchAdminActions(AuditAction action, String actorKeycloakId,
                                                             String targetId, Instant from, Instant to,
                                                             Pageable pageable) {
        Page<AuditLogResponse> page = auditLogs.search(action, actorKeycloakId, targetId, from, to, pageable)
                .map(mapper::toAuditLogResponse);

        // Resolve actor emails in one query
        Set<String> actorIds = page.getContent().stream()
                .map(AuditLogResponse::actorKeycloakId).collect(Collectors.toSet());
        Map<String, String> emails = users.findByKeycloakUserIdIn(actorIds).stream()
                .collect(Collectors.toMap(User::getKeycloakUserId, User::getEmail));

        Page<AuditLogResponse> enriched = page.map(a -> new AuditLogResponse(
                a.id(), a.action(), a.actorKeycloakId(), emails.get(a.actorKeycloakId()),
                a.targetType(), a.targetId(), a.details(), a.ipAddress(), a.createdAt()));
        return PageResponse.from(enriched);
    }

    public List<LoginEventResponse> loginEvents(String keycloakUserId, LocalDate from, LocalDate to,
                                                int page, int size) {
        return events.loginEvents(keycloakUserId, from, to, page, size);
    }
}
