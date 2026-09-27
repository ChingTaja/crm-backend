package com.taja.crm.crm_backend.dto.auth;

import com.taja.crm.crm_backend.model.User;

public record UserResponse(String id, String username, String email, RoleResponse role) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), RoleResponse.fromEntity(user.getRole()));
    }
}
