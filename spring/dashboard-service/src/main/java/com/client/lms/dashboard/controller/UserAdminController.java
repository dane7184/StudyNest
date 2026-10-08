package com.client.lms.dashboard.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import com.client.lms.common.api.PageResponse;
import com.client.lms.dashboard.dto.request.CreateInstructorRequest;
import com.client.lms.dashboard.dto.request.CreateStudentRequest;
import com.client.lms.dashboard.dto.request.UpdateUserRequest;
import com.client.lms.dashboard.dto.response.ClassroomSummaryResponse;
import com.client.lms.dashboard.dto.response.UserResponse;
import com.client.lms.dashboard.entity.Role;
import com.client.lms.dashboard.entity.UserStatus;
import com.client.lms.dashboard.service.UserAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users", description = "Create and manage instructor and student accounts (no public sign-up)")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService service;

    @Operation(summary = "Create one instructor. Keycloak emails a first-login password link.")
    @PostMapping("/instructors")
    public ResponseEntity<UserResponse> createInstructor(@Valid @RequestBody CreateInstructorRequest req) {
        UserResponse created = service.createInstructor(req);
        return ResponseEntity.created(URI.create("/api/admin/users/" + created.id())).body(created);
    }

    @Operation(summary = "Create one student. Optionally enrol by classroom code.")
    @PostMapping("/students")
    public ResponseEntity<UserResponse> createStudent(@Valid @RequestBody CreateStudentRequest req) {
        UserResponse created = service.createStudent(req);
        return ResponseEntity.created(URI.create("/api/admin/users/" + created.id())).body(created);
    }

    @Operation(summary = "Search users by name, email or student ID; filter by role and status")
    @GetMapping
    public PageResponse<UserResponse> search(@RequestParam(required = false) String q,
                                             @RequestParam(required = false) Role role,
                                             @RequestParam(required = false) UserStatus status,
                                             @PageableDefault(size = 20, sort = "createdAt",
                                                     direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(q, role, status, pageable);
    }

    @Operation(summary = "User detail, including lock and MFA state from Keycloak")
    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @Operation(summary = "Classrooms the user teaches or is enrolled in")
    @GetMapping("/{id}/classrooms")
    public List<ClassroomSummaryResponse> classrooms(@PathVariable UUID id) {
        return service.classroomsOf(id);
    }

    @PatchMapping("/{id}")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest req) {
        return service.update(id, req);
    }

    @Operation(summary = "Disable: user cannot log in; sessions are ended")
    @PostMapping("/{id}/disable")
    public ResponseEntity<Void> disable(@PathVariable UUID id) {
        service.setEnabled(id, false);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<Void> enable(@PathVariable UUID id) {
        service.setEnabled(id, true);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Soft delete (assumed): account disabled, data kept")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Resend the first-login password email")
    @PostMapping("/{id}/resend-invite")
    public ResponseEntity<Void> resendInvite(@PathVariable UUID id) {
        service.resendFirstLoginEmail(id);
        return ResponseEntity.noContent().build();
    }
}
