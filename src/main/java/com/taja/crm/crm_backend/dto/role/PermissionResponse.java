package com.taja.crm.crm_backend.dto.role;
@io.swagger.v3.oas.annotations.media.Schema(requiredProperties={"code","name","entity","groupName"})
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record PermissionResponse(String code,String name,String entity,String groupName,String description) {}
