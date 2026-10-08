package com.client.lms.dashboard.controller;

import java.util.UUID;

import com.client.lms.dashboard.service.AccountSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account security", description = "Unlock, reset MFA, force password reset (done in Keycloak)")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AccountSecurityController {

    private final AccountSecurityService service;

    @Operation(summary = "Clear the brute-force lockout")
    @PostMapping("/{id}/unlock")
    public ResponseEntity<Void> unlock(@PathVariable UUID id) {
        service.unlock(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove the OTP device; user sets it up again at next login")
    @PostMapping("/{id}/reset-mfa")
    public ResponseEntity<Void> resetMfa(@PathVariable UUID id) {
        service.resetMfa(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "End sessions and email a one-time password reset link")
    @PostMapping("/{id}/force-password-reset")
    public ResponseEntity<Void> forcePasswordReset(@PathVariable UUID id) {
        service.forcePasswordReset(id);
        return ResponseEntity.noContent().build();
    }
}
