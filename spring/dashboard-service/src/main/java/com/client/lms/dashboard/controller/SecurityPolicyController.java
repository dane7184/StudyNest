package com.client.lms.dashboard.controller;

import com.client.lms.dashboard.dto.request.MfaPolicyRequest;
import com.client.lms.dashboard.dto.request.PasswordPolicyRequest;
import com.client.lms.dashboard.dto.response.SecurityPolicyResponse;
import com.client.lms.dashboard.service.SecurityPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Security policy", description = "Password policy and MFA policy stored in Keycloak")
@RestController
@RequestMapping("/api/admin/security")
@RequiredArgsConstructor
public class SecurityPolicyController {

    private final SecurityPolicyService service;

    @GetMapping
    public SecurityPolicyResponse get() {
        return service.get();
    }

    @Operation(summary = "Update password rules and brute-force lockout")
    @PutMapping("/password-policy")
    public SecurityPolicyResponse updatePasswordPolicy(@Valid @RequestBody PasswordPolicyRequest req) {
        return service.updatePasswordPolicy(req);
    }

    @Operation(summary = "Turn MFA on/off and choose which roles must use it")
    @PutMapping("/mfa-policy")
    public SecurityPolicyResponse updateMfaPolicy(@Valid @RequestBody MfaPolicyRequest req) {
        return service.updateMfaPolicy(req);
    }
}
