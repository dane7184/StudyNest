package com.client.lms.common.security;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

/**
 * Reads Keycloak roles from the JWT and turns them into Spring authorities.
 * Realm roles:  realm_access.roles -> ROLE_<NAME>
 * Client roles: resource_access.<client>.roles -> ROLE_<NAME>
 */
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
    private final String clientId;

    public KeycloakJwtAuthenticationConverter(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                        scopes.convert(jwt).stream(),
                        Stream.concat(realmRoles(jwt).stream(), clientRoles(jwt).stream()))
                .collect(Collectors.toSet());
        String principal = jwt.getClaimAsString("preferred_username");
        return new JwtAuthenticationToken(jwt, authorities, principal != null ? principal : jwt.getSubject());
    }

    @SuppressWarnings("unchecked")
    private List<GrantedAuthority> realmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return Collections.emptyList();
        }
        return toAuthorities((Collection<String>) realmAccess.getOrDefault("roles", List.of()));
    }

    @SuppressWarnings("unchecked")
    private List<GrantedAuthority> clientRoles(Jwt jwt) {
        Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null || clientId == null) {
            return Collections.emptyList();
        }
        Object client = resourceAccess.get(clientId);
        if (!(client instanceof Map<?, ?> clientMap)) {
            return Collections.emptyList();
        }
        return toAuthorities((Collection<String>) ((Map<String, Object>) clientMap).getOrDefault("roles", List.of()));
    }

    private List<GrantedAuthority> toAuthorities(Collection<String> roles) {
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority(ROLE_PREFIX + r.toUpperCase()))
                .map(GrantedAuthority.class::cast)
                .toList();
    }
}
