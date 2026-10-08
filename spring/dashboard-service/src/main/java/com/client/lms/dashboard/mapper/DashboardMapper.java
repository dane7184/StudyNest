package com.client.lms.dashboard.mapper;

import com.client.lms.dashboard.dto.response.AuditLogResponse;
import com.client.lms.dashboard.dto.response.UserResponse;
import com.client.lms.dashboard.entity.AuditLog;
import com.client.lms.dashboard.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DashboardMapper {

    @Mapping(target = "locked", ignore = true)
    @Mapping(target = "mfaEnabled", ignore = true)
    UserResponse toUserResponse(User user);

    default UserResponse toUserResponse(User user, boolean locked, boolean mfaEnabled) {
        UserResponse base = toUserResponse(user);
        return new UserResponse(base.id(), base.keycloakUserId(), base.email(), base.fullName(),
                base.studentCode(), base.role(), base.status(), base.preferredLanguage(),
                locked, mfaEnabled, base.createdAt(), base.updatedAt());
    }

    @Mapping(target = "actorEmail", ignore = true)
    AuditLogResponse toAuditLogResponse(AuditLog log);
}
