package com.client.lms.dashboard.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Admin creates one instructor. Keycloak sends the first-login password reset email.
 */
public record CreateInstructorRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 150) String fullName,
        @Pattern(regexp = "km|en", message = "preferredLanguage must be km or en") String preferredLanguage
) {}
