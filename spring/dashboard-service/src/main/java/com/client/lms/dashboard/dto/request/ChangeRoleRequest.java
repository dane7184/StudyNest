package com.client.lms.dashboard.dto.request;

import com.client.lms.dashboard.entity.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull Role role) {}
