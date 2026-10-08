package com.client.lms.dashboard.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import com.client.lms.dashboard.config.KeycloakProperties;
import com.client.lms.dashboard.config.SecurityConfig;
import com.client.lms.dashboard.dto.response.UserResponse;
import com.client.lms.dashboard.entity.Role;
import com.client.lms.dashboard.entity.UserStatus;
import com.client.lms.dashboard.service.UserAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserAdminController.class)
@Import({SecurityConfig.class, UserAdminControllerTest.TestProps.class})
class UserAdminControllerTest {

    @Autowired MockMvc mvc;
    @MockBean UserAdminService service;
    @MockBean JwtDecoder jwtDecoder;

    static class TestProps {
        @org.springframework.context.annotation.Bean
        KeycloakProperties keycloakProperties() {
            return new KeycloakProperties("http://kc", "lms", "lms-web", "svc", "secret");
        }
    }

    @Test
    void createInstructor_requiresAdminRole() throws Exception {
        mvc.perform(post("/api/admin/users/instructors")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_STUDENT")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.co","fullName":"A B"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createInstructor_returns201_forAdmin() throws Exception {
        UserResponse res = new UserResponse(UUID.randomUUID(), "kc-1", "a@b.co", "A B", null,
                Role.INSTRUCTOR, UserStatus.ACTIVE, "en", null, null, Instant.now(), Instant.now());
        when(service.createInstructor(any())).thenReturn(res);

        mvc.perform(post("/api/admin/users/instructors")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.co","fullName":"A B"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("INSTRUCTOR"));
    }

    @Test
    void createInstructor_validatesEmail() throws Exception {
        mvc.perform(post("/api/admin/users/instructors")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","fullName":"A B"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
