package com.client.lms.dashboard.service;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.client.lms.dashboard.dto.request.MfaPolicyRequest;
import com.client.lms.dashboard.dto.request.PasswordPolicyRequest;
import com.client.lms.dashboard.dto.response.SecurityPolicyResponse;
import com.client.lms.dashboard.entity.AuditAction;
import com.client.lms.dashboard.entity.Role;
import com.client.lms.dashboard.event.AuditEventPublisher;
import com.client.lms.dashboard.integration.keycloak.KeycloakAdminClient;
import lombok.RequiredArgsConstructor;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.stereotype.Service;

/**
 * Use cases (Admin): view and change the password policy and MFA policy.
 * Keycloak holds the real settings; this service translates between the
 * admin UI form and the Keycloak realm representation.
 *
 * MFA policy per role is stored as realm attributes (assumed):
 *   lms.mfa.enabled = true|false
 *   lms.mfa.roles   = ADMIN,INSTRUCTOR
 * When enabled, Keycloak's "Configure OTP" required action is turned on, so
 * users in the listed roles are asked to set up OTP at their next login.
 */
@Service
@RequiredArgsConstructor
public class SecurityPolicyService {

    private static final String ATTR_MFA_ENABLED = "lms.mfa.enabled";
    private static final String ATTR_MFA_ROLES = "lms.mfa.roles";

    private final KeycloakAdminClient keycloak;
    private final AuditEventPublisher audit;

    public SecurityPolicyResponse get() {
        RealmRepresentation realm = keycloak.getRealm();
        return new SecurityPolicyResponse(readPasswordPolicy(realm), readMfaPolicy(realm));
    }

    public SecurityPolicyResponse updatePasswordPolicy(PasswordPolicyRequest req) {
        RealmRepresentation realm = keycloak.getRealm();

        realm.setPasswordPolicy(toPolicyString(req));
        realm.setBruteForceProtected(req.maxLoginFailures() > 0);
        realm.setFailureFactor(req.maxLoginFailures());
        realm.setMaxFailureWaitSeconds(req.lockoutMinutes() * 60);
        realm.setWaitIncrementSeconds(req.lockoutMinutes() * 60);
        realm.setPermanentLockout(false);

        keycloak.updateRealm(realm);
        audit.publish(AuditAction.PASSWORD_POLICY_UPDATED, "REALM", realm.getRealm(), toMap(req));
        return get();
    }

    public SecurityPolicyResponse updateMfaPolicy(MfaPolicyRequest req) {
        RealmRepresentation realm = keycloak.getRealm();
        Map<String, String> attrs = realm.getAttributes() != null
                ? new HashMap<>(realm.getAttributes()) : new HashMap<>();
        attrs.put(ATTR_MFA_ENABLED, String.valueOf(req.enabled()));
        attrs.put(ATTR_MFA_ROLES, req.requiredForRoles().stream().map(Enum::name)
                .sorted().collect(Collectors.joining(",")));
        realm.setAttributes(attrs);
        keycloak.updateRealm(realm);

        audit.publish(AuditAction.MFA_POLICY_UPDATED, "REALM", realm.getRealm(),
                Map.of("enabled", req.enabled(), "roles", attrs.get(ATTR_MFA_ROLES)));
        return get();
    }

    // ---------- password policy <-> Keycloak string ----------

    /** Example: length(8) and upperCase(1) and digits(1) and notUsername and passwordHistory(3) */
    static String toPolicyString(PasswordPolicyRequest req) {
        Map<String, Integer> rules = new LinkedHashMap<>();
        rules.put("length", req.minLength());
        rules.put("upperCase", req.minUpperCase());
        rules.put("lowerCase", req.minLowerCase());
        rules.put("digits", req.minDigits());
        rules.put("specialChars", req.minSpecialChars());
        rules.put("passwordHistory", req.passwordHistory());
        rules.put("forceExpiredPasswordChange", req.expireDays());

        StringBuilder sb = new StringBuilder();
        rules.forEach((name, value) -> {
            if (value != null && value > 0) {
                append(sb, name + "(" + value + ")");
            }
        });
        if (Boolean.TRUE.equals(req.notUsername())) {
            append(sb, "notUsername");
        }
        return sb.toString();
    }

    private static void append(StringBuilder sb, String rule) {
        if (!sb.isEmpty()) {
            sb.append(" and ");
        }
        sb.append(rule);
    }

    static SecurityPolicyResponse.PasswordPolicy readPasswordPolicy(RealmRepresentation realm) {
        Map<String, Integer> parsed = parsePolicy(realm.getPasswordPolicy());
        int lockoutMinutes = realm.getMaxFailureWaitSeconds() != null ? realm.getMaxFailureWaitSeconds() / 60 : 0;
        return new SecurityPolicyResponse.PasswordPolicy(
                parsed.getOrDefault("length", 0),
                parsed.getOrDefault("upperCase", 0),
                parsed.getOrDefault("lowerCase", 0),
                parsed.getOrDefault("digits", 0),
                parsed.getOrDefault("specialChars", 0),
                parsed.containsKey("notUsername"),
                parsed.getOrDefault("passwordHistory", 0),
                parsed.getOrDefault("forceExpiredPasswordChange", 0),
                Boolean.TRUE.equals(realm.isBruteForceProtected()) && realm.getFailureFactor() != null
                        ? realm.getFailureFactor() : 0,
                lockoutMinutes);
    }

    static Map<String, Integer> parsePolicy(String policy) {
        Map<String, Integer> out = new HashMap<>();
        if (policy == null || policy.isBlank()) {
            return out;
        }
        for (String rule : policy.split(" and ")) {
            String r = rule.trim();
            int open = r.indexOf('(');
            if (open < 0) {
                out.put(r, 1);
            } else {
                String name = r.substring(0, open);
                String val = r.substring(open + 1, r.indexOf(')'));
                try {
                    out.put(name, Integer.parseInt(val));
                } catch (NumberFormatException ignored) {
                    out.put(name, 1);
                }
            }
        }
        return out;
    }

    static SecurityPolicyResponse.MfaPolicy readMfaPolicy(RealmRepresentation realm) {
        Map<String, String> attrs = realm.getAttributes() != null ? realm.getAttributes() : Map.of();
        boolean enabled = Boolean.parseBoolean(attrs.getOrDefault(ATTR_MFA_ENABLED, "false"));
        String roles = attrs.getOrDefault(ATTR_MFA_ROLES, "");
        Set<Role> set = roles.isBlank() ? EnumSet.noneOf(Role.class)
                : Arrays.stream(roles.split(",")).map(String::trim).map(Role::valueOf)
                        .collect(Collectors.toCollection(() -> EnumSet.noneOf(Role.class)));
        return new SecurityPolicyResponse.MfaPolicy(enabled, set);
    }

    private static Map<String, Object> toMap(PasswordPolicyRequest req) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("minLength", req.minLength());
        m.put("minUpperCase", req.minUpperCase());
        m.put("minLowerCase", req.minLowerCase());
        m.put("minDigits", req.minDigits());
        m.put("minSpecialChars", req.minSpecialChars());
        m.put("notUsername", req.notUsername());
        m.put("passwordHistory", req.passwordHistory());
        m.put("expireDays", req.expireDays());
        m.put("maxLoginFailures", req.maxLoginFailures());
        m.put("lockoutMinutes", req.lockoutMinutes());
        return m;
    }
}
