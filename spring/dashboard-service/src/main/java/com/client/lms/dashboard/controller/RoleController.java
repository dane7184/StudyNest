package com.client.lms.dashboard.controller;

import java.util.UUID;

import com.client.lms.dashboard.dto.request.ChangeRoleRequest;
import com.client.lms.dashboard.dto.response.UserResponse;
import com.client.lms.dashboard.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Roles")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService service;

    @Operation(summary = "Change a user's role (ADMIN / INSTRUCTOR / STUDENT). Sessions are ended.")
    @PutMapping("/{id}/role")
    public UserResponse changeRole(@PathVariable UUID id, @Valid @RequestBody ChangeRoleRequest req) {
        return service.changeRole(id, req);
    }
}
