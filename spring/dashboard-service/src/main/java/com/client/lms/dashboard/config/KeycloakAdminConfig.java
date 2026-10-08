package com.client.lms.dashboard.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakAdminConfig {

    @Bean(destroyMethod = "close")
    public Keycloak keycloak(KeycloakProperties props) {
        return KeycloakBuilder.builder()
                .serverUrl(props.serverUrl())
                .realm(props.realm())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(props.adminClientId())
                .clientSecret(props.adminClientSecret())
                .build();
    }

    @Bean
    public RealmResource realmResource(Keycloak keycloak, KeycloakProperties props) {
        return keycloak.realm(props.realm());
    }
}
