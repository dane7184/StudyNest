package com.client.lms.dashboard.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.client.lms.common.api.PageResponse;
import com.client.lms.common.exception.ResourceNotFoundException;
import com.client.lms.dashboard.dto.request.CreateInstructorRequest;
import com.client.lms.dashboard.dto.request.CreateStudentRequest;
import com.client.lms.dashboard.dto.request.UpdateUserRequest;
import com.client.lms.dashboard.dto.response.ClassroomSummaryResponse;
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
import com.client.lms.dashboard.repository.UserRepository;
import com.client.lms.dashboard.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases (Admin): create instructor, create student, list / search users,
 * view user, update, disable / enable, delete (soft, assumed).
 * There is no public sign-up: every account is created here or by Excel import.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private static final String DEFAULT_LANGUAGE = "en";
    private static final String TARGET_USER = "USER";

    private final UserRepository users;
    private final KeycloakAdminClient keycloak;
    private final ClassroomServiceClient classrooms;
    private final DashboardMapper mapper;
    private final AuditEventPublisher audit;
    private final CurrentUser currentUser;

    @Transactional
    public UserResponse createInstructor(CreateInstructorRequest req) {
        User user = create(req.email(), req.fullName(), null, Role.INSTRUCTOR, req.preferredLanguage());
        return mapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse createStudent(CreateStudentRequest req) {
        User user = create(req.email(), req.fullName(), req.studentCode(), Role.STUDENT, req.preferredLanguage());
        if (req.classroomCode() != null && !req.classroomCode().isBlank()) {
            classrooms.enrolByCode(user.getId(), req.classroomCode().trim());
        }
        return mapper.toUserResponse(user);
    }

    /** Shared by single-create and Excel import. */
    @Transactional
    public User create(String email, String fullName, String studentCode, Role role, String preferredLanguage) {
        String normalisedEmail = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(normalisedEmail)) {
            throw new UserAlreadyExistsException(normalisedEmail);
        }
        if (studentCode != null && !studentCode.isBlank() && users.existsByStudentCode(studentCode.trim())) {
            throw new UserAlreadyExistsException("student code " + studentCode);
        }
        String language = preferredLanguage != null ? preferredLanguage : DEFAULT_LANGUAGE;

        // 1. Keycloak first: it owns the identity and sends the first-login email.
        String keycloakId = keycloak.createUser(normalisedEmail, fullName.trim(), role, language);

        // 2. Local profile row, linked by keycloak_user_id. No password is stored.
        User user = User.builder()
                .id(UUID.randomUUID())
                .keycloakUserId(keycloakId)
                .email(normalisedEmail)
                .fullName(fullName.trim())
                .studentCode(studentCode != null && !studentCode.isBlank() ? studentCode.trim() : null)
                .role(role)
                .status(UserStatus.ACTIVE)
                .preferredLanguage(language)
                .build();
        user = users.save(user);

        audit.publish(AuditAction.USER_CREATED, TARGET_USER, user.getId(),
                Map.of("email", normalisedEmail, "role", role.name()));
        return user;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String q, Role role, UserStatus status, Pageable pageable) {
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return PageResponse.from(users.search(query, role, status, pageable).map(mapper::toUserResponse));
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        User user = find(id);
        boolean locked = keycloak.isLocked(user.getKeycloakUserId());
        boolean mfa = keycloak.hasMfa(user.getKeycloakUserId());
        return mapper.toUserResponse(user, locked, mfa);
    }

    @Transactional(readOnly = true)
    public List<ClassroomSummaryResponse> classroomsOf(UUID id) {
        find(id);
        return classrooms.classroomsOfUser(id);
    }

    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest req) {
        User user = find(id);
        if (req.fullName() != null && !req.fullName().isBlank()) {
            user.setFullName(req.fullName().trim());
            keycloak.updateName(user.getKeycloakUserId(), user.getFullName());
        }
        if (req.studentCode() != null) {
            user.setStudentCode(req.studentCode().isBlank() ? null : req.studentCode().trim());
        }
        if (req.preferredLanguage() != null) {
            user.setPreferredLanguage(req.preferredLanguage());
        }
        audit.publish(AuditAction.USER_UPDATED, TARGET_USER, id, null);
        return mapper.toUserResponse(users.save(user));
    }

    @Transactional
    public void setEnabled(UUID id, boolean enabled) {
        User user = find(id);
        guardNotSelf(user);
        keycloak.setEnabled(user.getKeycloakUserId(), enabled);
        user.setStatus(enabled ? UserStatus.ACTIVE : UserStatus.DISABLED);
        users.save(user);
        audit.publish(enabled ? AuditAction.USER_ENABLED : AuditAction.USER_DISABLED, TARGET_USER, id, null);
    }

    /**
     * Soft delete (assumed, open decision): the Keycloak account is disabled so the
     * user cannot log in, and the local row is marked DELETED. Data is kept.
     */
    @Transactional
    public void delete(UUID id) {
        User user = find(id);
        guardNotSelf(user);
        keycloak.setEnabled(user.getKeycloakUserId(), false);
        user.setStatus(UserStatus.DELETED);
        user.setDeletedAt(Instant.now());
        users.save(user);
        audit.publish(AuditAction.USER_DELETED, TARGET_USER, id, Map.of("mode", "soft"));
    }

    @Transactional
    public void resendFirstLoginEmail(UUID id) {
        User user = find(id);
        keycloak.sendFirstLoginEmail(user.getKeycloakUserId());
    }

    User find(UUID id) {
        return users.findById(id)
                .filter(u -> u.getStatus() != UserStatus.DELETED)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    void guardNotSelf(User user) {
        if (user.getKeycloakUserId().equals(currentUser.keycloakId())) {
            throw new SelfModificationException();
        }
    }
}
