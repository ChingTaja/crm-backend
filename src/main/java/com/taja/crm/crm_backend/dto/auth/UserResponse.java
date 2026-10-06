package com.taja.crm.crm_backend.dto.auth;

import com.taja.crm.crm_backend.model.User;

public record UserResponse(String id, String username, String email, RoleResponse role,
        @io.swagger.v3.oas.annotations.media.Schema(requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED) boolean enabled) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), RoleResponse.fromEntity(user.getRole()), user.isEnabled());
    }
}
