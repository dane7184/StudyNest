package com.client.lms.dashboard.service;

import java.util.Map;
import java.util.UUID;

import com.client.lms.dashboard.dto.request.ChangeRoleRequest;
import com.client.lms.dashboard.dto.response.UserResponse;
import com.client.lms.dashboard.entity.AuditAction;
import com.client.lms.dashboard.entity.Role;
import com.client.lms.dashboard.entity.User;
import com.client.lms.dashboard.event.AuditEventPublisher;
import com.client.lms.dashboard.integration.keycloak.KeycloakAdminClient;
import com.client.lms.dashboard.mapper.DashboardMapper;
import com.client.lms.dashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use case (Admin): change a user's role. The role is stored in Keycloak (realm role)
 * and mirrored in users.role so lists can filter without calling Keycloak.
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final UserRepository users;
    private final UserAdminService userAdmin;
    private final KeycloakAdminClient keycloak;
    private final DashboardMapper mapper;
    private final AuditEventPublisher audit;

    @Transactional
    public UserResponse changeRole(UUID id, ChangeRoleRequest req) {
        User user = userAdmin.find(id);
        userAdmin.guardNotSelf(user);
        Role oldRole = user.getRole();
        Role newRole = req.role();
        if (oldRole == newRole) {
            return mapper.toUserResponse(user);
        }
        keycloak.replaceRealmRole(user.getKeycloakUserId(), oldRole, newRole);
        user.setRole(newRole);
        users.save(user);
        audit.publish(AuditAction.USER_ROLE_CHANGED, "USER", id,
                Map.of("from", oldRole.name(), "to", newRole.name()));
        return mapper.toUserResponse(user);
    }
}
