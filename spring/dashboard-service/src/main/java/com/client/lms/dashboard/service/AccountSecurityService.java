package com.client.lms.dashboard.service;

import java.util.UUID;

import com.client.lms.dashboard.entity.AuditAction;
import com.client.lms.dashboard.entity.User;
import com.client.lms.dashboard.event.AuditEventPublisher;
import com.client.lms.dashboard.integration.keycloak.KeycloakAdminClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases (Admin): unlock a locked account, reset MFA, force a password reset.
 * All three are done in Keycloak; the service only records the audit entry.
 */
@Service
@RequiredArgsConstructor
public class AccountSecurityService {

    private static final String TARGET_USER = "USER";

    private final UserAdminService userAdmin;
    private final KeycloakAdminClient keycloak;
    private final AuditEventPublisher audit;

    /** Clears the brute-force lockout. */
    @Transactional(readOnly = true)
    public void unlock(UUID id) {
        User user = userAdmin.find(id);
        keycloak.unlock(user.getKeycloakUserId());
        audit.publish(AuditAction.USER_UNLOCKED, TARGET_USER, id, null);
    }

    /** Removes the user's OTP device so they set it up again at next login. */
    @Transactional(readOnly = true)
    public void resetMfa(UUID id) {
        User user = userAdmin.find(id);
        keycloak.resetMfa(user.getKeycloakUserId());
        audit.publish(AuditAction.USER_MFA_RESET, TARGET_USER, id, null);
    }

    /** Ends sessions and emails a one-time password-reset link. */
    @Transactional(readOnly = true)
    public void forcePasswordReset(UUID id) {
        User user = userAdmin.find(id);
        keycloak.forcePasswordReset(user.getKeycloakUserId());
        audit.publish(AuditAction.USER_PASSWORD_RESET_FORCED, TARGET_USER, id, null);
    }
}
