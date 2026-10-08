package com.client.lms.dashboard.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(max = 150) String fullName,
        @Size(max = 50) String studentCode,
        @Pattern(regexp = "km|en", message = "preferredLanguage must be km or en") String preferredLanguage
) {}
