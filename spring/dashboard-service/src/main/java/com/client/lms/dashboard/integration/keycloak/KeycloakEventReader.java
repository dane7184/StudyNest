package com.client.lms.dashboard.integration.keycloak;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import com.client.lms.dashboard.dto.response.LoginEventResponse;
import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.EventRepresentation;
import org.springframework.stereotype.Component;

/**
 * Reads login audit events from Keycloak.
 * Keycloak must have "Save events" enabled for LOGIN, LOGIN_ERROR, LOGOUT,
 * UPDATE_PASSWORD, RESET_PASSWORD (set in realm-export.json).
 */
@Component
@RequiredArgsConstructor
public class KeycloakEventReader {

    private static final List<String> LOGIN_TYPES =
            List.of("LOGIN", "LOGIN_ERROR", "LOGOUT", "UPDATE_PASSWORD", "RESET_PASSWORD", "SEND_RESET_PASSWORD");

    private final RealmResource realm;

    public List<LoginEventResponse> loginEvents(String keycloakUserId, LocalDate from, LocalDate to,
                                                int page, int size) {
        List<EventRepresentation> events = realm.getEvents(
                LOGIN_TYPES,
                null,                       // client
                keycloakUserId,
                from != null ? from.toString() : null,
                to != null ? to.toString() : null,
                null,                       // ip
                page * size,
                size);
        return events.stream().map(KeycloakEventReader::toResponse).toList();
    }

    public long countByType(String type, int lastDays) {
        LocalDate from = LocalDate.now(ZoneOffset.UTC).minusDays(lastDays);
        List<EventRepresentation> events = realm.getEvents(
                List.of(type), null, null, from.toString(), null, null, 0, 10_000);
        return events.size();
    }

    private static LoginEventResponse toResponse(EventRepresentation e) {
        String username = e.getDetails() != null ? e.getDetails().get("username") : null;
        return new LoginEventResponse(
                Instant.ofEpochMilli(e.getTime()),
                e.getType(),
                e.getUserId(),
                username,
                e.getIpAddress(),
                e.getError());
    }
}
