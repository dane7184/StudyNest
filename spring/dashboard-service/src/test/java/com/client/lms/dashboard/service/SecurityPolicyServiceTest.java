package com.client.lms.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.client.lms.dashboard.dto.request.PasswordPolicyRequest;
import org.junit.jupiter.api.Test;

class SecurityPolicyServiceTest {

    @Test
    void toPolicyString_buildsKeycloakFormat() {
        PasswordPolicyRequest req = new PasswordPolicyRequest(8, 1, 1, 1, 0, true, 3, 0, 5, 15);

        String policy = SecurityPolicyService.toPolicyString(req);

        assertThat(policy).isEqualTo(
                "length(8) and upperCase(1) and lowerCase(1) and digits(1) and passwordHistory(3) and notUsername");
    }

    @Test
    void parsePolicy_roundTrips() {
        Map<String, Integer> parsed = SecurityPolicyService.parsePolicy(
                "length(10) and digits(2) and notUsername");

        assertThat(parsed).containsEntry("length", 10)
                .containsEntry("digits", 2)
                .containsEntry("notUsername", 1);
    }
}
