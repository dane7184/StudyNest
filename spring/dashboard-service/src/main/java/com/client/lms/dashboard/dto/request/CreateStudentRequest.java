package com.client.lms.dashboard.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Admin creates one student. The same fields are used per Excel row.
 */
public record CreateStudentRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(max = 150) String fullName,
        @Size(max = 50) String studentCode,
        /** Optional classroom invite code to enrol the student right away (assumed). */
        @Size(max = 20) String classroomCode,
        @Pattern(regexp = "km|en", message = "preferredLanguage must be km or en") String preferredLanguage
) {}
