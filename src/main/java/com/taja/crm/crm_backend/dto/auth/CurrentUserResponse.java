package com.taja.crm.crm_backend.dto.auth;
import java.util.List;
@io.swagger.v3.oas.annotations.media.Schema(requiredProperties={"id","username","email","role","permissionCodes"})
public record CurrentUserResponse(String id,String username,String email,RoleResponse role,List<String> permissionCodes) {}
