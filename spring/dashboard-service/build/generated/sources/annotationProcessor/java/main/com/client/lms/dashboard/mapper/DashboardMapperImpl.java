package com.client.lms.dashboard.mapper;

import com.client.lms.dashboard.dto.response.AuditLogResponse;
import com.client.lms.dashboard.dto.response.UserResponse;
import com.client.lms.dashboard.entity.AuditAction;
import com.client.lms.dashboard.entity.AuditLog;
import com.client.lms.dashboard.entity.Role;
import com.client.lms.dashboard.entity.User;
import com.client.lms.dashboard.entity.UserStatus;
import java.time.Instant;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-08T22:37:34+0700",
    comments = "version: 1.6.2, compiler: IncrementalProcessingEnvironment from gradle-language-java-8.10.2.jar, environment: Java 21.0.12.1 (Microsoft)"
)
@Component
public class DashboardMapperImpl implements DashboardMapper {

    @Override
    public UserResponse toUserResponse(User user) {
        if ( user == null ) {
            return null;
        }

        UUID id = null;
        String keycloakUserId = null;
        String email = null;
        String fullName = null;
        String studentCode = null;
        Role role = null;
        UserStatus status = null;
        String preferredLanguage = null;
        Instant createdAt = null;
        Instant updatedAt = null;

        id = user.getId();
        keycloakUserId = user.getKeycloakUserId();
        email = user.getEmail();
        fullName = user.getFullName();
        studentCode = user.getStudentCode();
        role = user.getRole();
        status = user.getStatus();
        preferredLanguage = user.getPreferredLanguage();
        createdAt = user.getCreatedAt();
        updatedAt = user.getUpdatedAt();

        Boolean locked = null;
        Boolean mfaEnabled = null;

        UserResponse userResponse = new UserResponse( id, keycloakUserId, email, fullName, studentCode, role, status, preferredLanguage, locked, mfaEnabled, createdAt, updatedAt );

        return userResponse;
    }

    @Override
    public AuditLogResponse toAuditLogResponse(AuditLog log) {
        if ( log == null ) {
            return null;
        }

        UUID id = null;
        AuditAction action = null;
        String actorKeycloakId = null;
        String targetType = null;
        String targetId = null;
        String details = null;
        String ipAddress = null;
        Instant createdAt = null;

        id = log.getId();
        action = log.getAction();
        actorKeycloakId = log.getActorKeycloakId();
        targetType = log.getTargetType();
        targetId = log.getTargetId();
        details = log.getDetails();
        ipAddress = log.getIpAddress();
        createdAt = log.getCreatedAt();

        String actorEmail = null;

        AuditLogResponse auditLogResponse = new AuditLogResponse( id, action, actorKeycloakId, actorEmail, targetType, targetId, details, ipAddress, createdAt );

        return auditLogResponse;
    }
}
