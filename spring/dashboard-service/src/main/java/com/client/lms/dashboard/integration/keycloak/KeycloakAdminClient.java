package com.client.lms.dashboard.integration.keycloak;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.client.lms.dashboard.entity.Role;
import com.client.lms.dashboard.exception.KeycloakOperationException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Component;

/**
 * All identity work goes through Keycloak. This class wraps the Admin API.
 * The app never stores or sees user passwords.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakAdminClient {

    private static final String REQUIRED_ACTION_UPDATE_PASSWORD = "UPDATE_PASSWORD";
    private static final String REQUIRED_ACTION_CONFIGURE_TOTP = "CONFIGURE_TOTP";
    private static final String CREDENTIAL_TYPE_OTP = "otp";

    private final RealmResource realm;

    // ---------- Users ----------

    /**
     * Creates the user, assigns the realm role, and sets the "update password" required
     * action so Keycloak forces a password reset on first login. Keycloak then sends the
     * "set up your account" email with a one-time link.
     *
     * @return the new Keycloak user id
     */
    public String createUser(String email, String fullName, Role role, String preferredLanguage) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(email.toLowerCase());
        user.setEmail(email.toLowerCase());
        user.setEnabled(true);
        user.setEmailVerified(false);
        splitName(fullName, user);
        user.setAttributes(Map.of("locale", List.of(preferredLanguage)));
        user.setRequiredActions(List.of(REQUIRED_ACTION_UPDATE_PASSWORD));

        try (Response response = realm.users().create(user)) {
            if (response.getStatus() == 409) {
                throw new KeycloakOperationException("Keycloak already has a user with email " + email);
            }
            if (response.getStatus() != 201) {
                throw new KeycloakOperationException("Keycloak could not create the user (HTTP " + response.getStatus() + ")");
            }
            String userId = extractId(response.getLocation());
            assignRealmRole(userId, role);
            sendFirstLoginEmail(userId);
            return userId;
        }
    }

    /** Sends the first-login "set up password" email. */
    public void sendFirstLoginEmail(String keycloakUserId) {
        try {
            userResource(keycloakUserId).executeActionsEmail(List.of(REQUIRED_ACTION_UPDATE_PASSWORD));
        } catch (Exception ex) {
            // Do not fail account creation if mail is down; admin can resend.
            log.warn("Could not send first-login email to user {}: {}", keycloakUserId, ex.getMessage());
        }
    }

    public void updateName(String keycloakUserId, String fullName) {
        UserResource res = userResource(keycloakUserId);
        UserRepresentation rep = res.toRepresentation();
        splitName(fullName, rep);
        res.update(rep);
    }

    public void setEnabled(String keycloakUserId, boolean enabled) {
        UserResource res = userResource(keycloakUserId);
        UserRepresentation rep = res.toRepresentation();
        rep.setEnabled(enabled);
        res.update(rep);
        if (!enabled) {
            res.logout(); // end active sessions
        }
    }

    public void deleteUser(String keycloakUserId) {
        try (Response response = realm.users().delete(keycloakUserId)) {
            if (response.getStatus() >= 300) {
                throw new KeycloakOperationException("Keycloak could not delete the user");
            }
        }
    }

    public Optional<UserRepresentation> findUser(String keycloakUserId) {
        try {
            return Optional.of(userResource(keycloakUserId).toRepresentation());
        } catch (jakarta.ws.rs.NotFoundException ex) {
            return Optional.empty();
        }
    }

    // ---------- Roles ----------

    public void assignRealmRole(String keycloakUserId, Role role) {
        RoleRepresentation rep = realm.roles().get(role.name()).toRepresentation();
        userResource(keycloakUserId).roles().realmLevel().add(List.of(rep));
    }

    public void replaceRealmRole(String keycloakUserId, Role oldRole, Role newRole) {
        UserResource res = userResource(keycloakUserId);
        RoleRepresentation oldRep = realm.roles().get(oldRole.name()).toRepresentation();
        RoleRepresentation newRep = realm.roles().get(newRole.name()).toRepresentation();
        res.roles().realmLevel().remove(List.of(oldRep));
        res.roles().realmLevel().add(List.of(newRep));
        res.logout(); // new role takes effect on next login
    }

    // ---------- Account security ----------

    /** Clears a brute-force lockout so the user can log in again. */
    public void unlock(String keycloakUserId) {
        realm.attackDetection().clearBruteForceForUser(keycloakUserId);
    }

    /** True if brute-force protection currently blocks this user. */
    public boolean isLocked(String keycloakUserId) {
        Map<String, Object> status = realm.attackDetection().bruteForceUserStatus(keycloakUserId);
        Object disabled = status.get("disabled");
        return Boolean.TRUE.equals(disabled);
    }

    public boolean hasMfa(String keycloakUserId) {
        return userResource(keycloakUserId).credentials().stream()
                .anyMatch(c -> CREDENTIAL_TYPE_OTP.equals(c.getType()));
    }

    /** Removes the OTP credential and asks the user to set it up again at next login. */
    public void resetMfa(String keycloakUserId) {
        UserResource res = userResource(keycloakUserId);
        res.credentials().stream()
                .filter(c -> CREDENTIAL_TYPE_OTP.equals(c.getType()))
                .map(CredentialRepresentation::getId)
                .forEach(res::removeCredential);
        UserRepresentation rep = res.toRepresentation();
        rep.setRequiredActions(List.of(REQUIRED_ACTION_CONFIGURE_TOTP));
        res.update(rep);
        res.logout();
    }

    /** Forces a password reset at next login and sends the reset email. */
    public void forcePasswordReset(String keycloakUserId) {
        UserResource res = userResource(keycloakUserId);
        UserRepresentation rep = res.toRepresentation();
        rep.setRequiredActions(List.of(REQUIRED_ACTION_UPDATE_PASSWORD));
        res.update(rep);
        res.logout();
        res.executeActionsEmail(List.of(REQUIRED_ACTION_UPDATE_PASSWORD));
    }

    // ---------- Realm policy ----------

    public RealmRepresentation getRealm() {
        return realm.toRepresentation();
    }

    public void updateRealm(RealmRepresentation rep) {
        realm.update(rep);
    }

    // ---------- helpers ----------

    private UserResource userResource(String keycloakUserId) {
        return realm.users().get(keycloakUserId);
    }

    private static void splitName(String fullName, UserRepresentation user) {
        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        if (space < 0) {
            user.setFirstName(trimmed);
            user.setLastName("");
        } else {
            user.setFirstName(trimmed.substring(0, space));
            user.setLastName(trimmed.substring(space + 1));
        }
    }

    private static String extractId(URI location) {
        String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
