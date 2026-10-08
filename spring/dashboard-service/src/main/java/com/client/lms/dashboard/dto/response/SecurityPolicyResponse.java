package com.client.lms.dashboard.dto.response;

import java.util.Set;

import com.client.lms.dashboard.entity.Role;

public record SecurityPolicyResponse(
        PasswordPolicy passwordPolicy,
        MfaPolicy mfaPolicy
) {
    public record PasswordPolicy(
            int minLength, int minUpperCase, int minLowerCase, int minDigits, int minSpecialChars,
            boolean notUsername, int passwordHistory, int expireDays,
            int maxLoginFailures, int lockoutMinutes) {}

    public record MfaPolicy(boolean enabled, Set<Role> requiredForRoles) {}
}
