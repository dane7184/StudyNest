package com.client.lms.dashboard.exception;

import com.client.lms.common.exception.ApiException;
import org.springframework.http.HttpStatus;

/** Thrown when a Keycloak Admin API call fails. */
public class KeycloakOperationException extends ApiException {

    public KeycloakOperationException(String message) {
        super(HttpStatus.BAD_GATEWAY, "KEYCLOAK_ERROR", message);
    }
}
