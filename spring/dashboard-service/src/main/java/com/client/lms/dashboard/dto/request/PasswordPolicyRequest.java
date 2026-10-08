package com.client.lms.dashboard.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Password policy stored in Keycloak (realm passwordPolicy string).
 * Values below are the defaults (assumed) until the client confirms.
 */
public record PasswordPolicyRequest(
        @NotNull @Min(6) @Max(64) Integer minLength,
        @NotNull @Min(0) @Max(10) Integer minUpperCase,
        @NotNull @Min(0) @Max(10) Integer minLowerCase,
        @NotNull @Min(0) @Max(10) Integer minDigits,
        @NotNull @Min(0) @Max(10) Integer minSpecialChars,
        /** Keycloak "notUsername" rule. */
        @NotNull Boolean notUsername,
        /** How many old passwords may not be reused. 0 = off. */
        @NotNull @Min(0) @Max(24) Integer passwordHistory,
        /** Days before a password expires. 0 = never. */
        @NotNull @Min(0) @Max(365) Integer expireDays,
        /** Brute-force: failures before the account is locked. 0 = off. */
        @NotNull @Min(0) @Max(30) Integer maxLoginFailures,
        /** Brute-force: lock time in minutes. */
        @NotNull @Min(1) @Max(1440) Integer lockoutMinutes
) {}
