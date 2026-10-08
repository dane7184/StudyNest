package com.client.lms.dashboard.dto.request;

import java.util.Set;

import com.client.lms.dashboard.entity.Role;
import jakarta.validation.constraints.NotNull;

/**
 * MFA (OTP) policy. "requiredForRoles" lists the roles that must set up OTP at next login.
 */
public record MfaPolicyRequest(
        @NotNull Boolean enabled,
        @NotNull Set<Role> requiredForRoles
) {}
