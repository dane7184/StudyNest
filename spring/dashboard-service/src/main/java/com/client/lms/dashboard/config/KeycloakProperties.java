package com.client.lms.dashboard.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Keycloak connection settings. The service calls the Keycloak Admin API with a
 * service account (client credentials). It never stores user passwords.
 */
@Validated
@ConfigurationProperties(prefix = "lms.keycloak")
public record KeycloakProperties(
        @NotBlank String serverUrl,
        @NotBlank String realm,
        /** Client id used by the web/mobile apps. Used to read client roles from the JWT. */
        @NotBlank String appClientId,
        /** Service-account client used for Admin API calls. */
        @NotBlank String adminClientId,
        @NotBlank String adminClientSecret
) {}
