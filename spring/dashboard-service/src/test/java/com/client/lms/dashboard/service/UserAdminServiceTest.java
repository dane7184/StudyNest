package com.client.lms.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.client.lms.dashboard.dto.request.CreateInstructorRequest;
import com.client.lms.dashboard.dto.response.UserResponse;
import com.client.lms.dashboard.entity.AuditAction;
import com.client.lms.dashboard.entity.Role;
import com.client.lms.dashboard.entity.User;
import com.client.lms.dashboard.entity.UserStatus;
import com.client.lms.dashboard.event.AuditEventPublisher;
import com.client.lms.dashboard.exception.SelfModificationException;
import com.client.lms.dashboard.exception.UserAlreadyExistsException;
import com.client.lms.dashboard.integration.client.ClassroomServiceClient;
import com.client.lms.dashboard.integration.keycloak.KeycloakAdminClient;
import com.client.lms.dashboard.mapper.DashboardMapper;
import com.client.lms.dashboard.mapper.DashboardMapperImpl;
import com.client.lms.dashboard.repository.UserRepository;
import com.client.lms.dashboard.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock UserRepository users;
    @Mock KeycloakAdminClient keycloak;
    @Mock ClassroomServiceClient classrooms;
    @Mock AuditEventPublisher audit;
    @Mock CurrentUser currentUser;

    DashboardMapper mapper = new DashboardMapperImpl();
    UserAdminService service;

    @BeforeEach
    void setUp() {
        service = new UserAdminService(users, keycloak, classrooms, mapper, audit, currentUser);
    }

    @Test
    void createInstructor_createsInKeycloakFirst_thenLocalRow_withoutPassword() {
        when(users.existsByEmailIgnoreCase("ann@school.edu")).thenReturn(false);
        when(keycloak.createUser(eq("ann@school.edu"), eq("Ann Chan"), eq(Role.INSTRUCTOR), eq("km")))
                .thenReturn("kc-123");
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse res = service.createInstructor(
                new CreateInstructorRequest("Ann@School.edu", "Ann Chan", "km"));

        assertThat(res.email()).isEqualTo("ann@school.edu");
        assertThat(res.keycloakUserId()).isEqualTo("kc-123");
        assertThat(res.role()).isEqualTo(Role.INSTRUCTOR);
        assertThat(res.status()).isEqualTo(UserStatus.ACTIVE);
        verify(audit).publish(eq(AuditAction.USER_CREATED), eq("USER"), any(UUID.class), any());
    }

    @Test
    void create_rejectsDuplicateEmail_beforeCallingKeycloak() {
        when(users.existsByEmailIgnoreCase("dup@school.edu")).thenReturn(true);

        assertThatThrownBy(() -> service.create("dup@school.edu", "Dup", null, Role.STUDENT, "en"))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(keycloak, never()).createUser(anyString(), anyString(), any(), anyString());
    }

    @Test
    void disable_refusesOwnAccount() {
        UUID id = UUID.randomUUID();
        User me = User.builder().id(id).keycloakUserId("kc-me").email("me@x").fullName("Me")
                .role(Role.ADMIN).status(UserStatus.ACTIVE).preferredLanguage("en").build();
        when(users.findById(id)).thenReturn(java.util.Optional.of(me));
        when(currentUser.keycloakId()).thenReturn("kc-me");

        assertThatThrownBy(() -> service.setEnabled(id, false))
                .isInstanceOf(SelfModificationException.class);
    }
}
