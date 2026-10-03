package com.taja.crm.crm_backend.dto.auth;

import com.taja.crm.crm_backend.model.Role;

@io.swagger.v3.oas.annotations.media.Schema(requiredProperties={"id","code","name"})
public record RoleResponse(String id, String code, String name) {
    public static RoleResponse fromEntity(Role role) {
        return role == null ? null : new RoleResponse(role.getId(), role.getCode(), role.getName());
    }
}
