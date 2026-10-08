package com.client.lms.dashboard.event;

import java.util.Map;
import java.util.UUID;

import com.client.lms.dashboard.entity.AuditAction;
import com.client.lms.dashboard.entity.AuditLog;
import com.client.lms.dashboard.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes one audit_log row for every admin action.
 * Uses REQUIRES_NEW so the audit row is kept even if the caller's transaction rolls back.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventPublisher {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<HttpServletRequest> requestProvider;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publish(AuditAction action, String targetType, String targetId, Map<String, Object> details) {
        AuditLog entry = AuditLog.builder()
                .id(UUID.randomUUID())
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(toJson(details))
                .ipAddress(clientIp())
                .build();
        repository.save(entry);
    }

    public void publish(AuditAction action, String targetType, UUID targetId, Map<String, Object> details) {
        publish(action, targetType, targetId != null ? targetId.toString() : null, details);
    }

    private String toJson(Map<String, Object> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialise audit details", e);
            return details.toString();
        }
    }

    private String clientIp() {
        HttpServletRequest request = requestProvider.getIfAvailable();
        if (request == null) {
            return null; // async job, no request
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
