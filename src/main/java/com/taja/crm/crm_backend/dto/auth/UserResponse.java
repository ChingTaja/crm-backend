package com.taja.crm.crm_backend.dto.auth;

import com.taja.crm.crm_backend.model.User;

public record UserResponse(String id, String username, String email) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail());
    }
}
