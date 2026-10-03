package com.taja.crm.crm_backend.dto.role;
@io.swagger.v3.oas.annotations.media.Schema(requiredProperties={"id","code","name"})
public record RoleOptionResponse(String id,String code,String name) {}
