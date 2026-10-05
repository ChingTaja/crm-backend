package com.taja.crm.crm_backend.dto.role;
@io.swagger.v3.oas.annotations.media.Schema(requiredProperties={"id","code","name","system","userCount","permissionCount","revision","quotePermissionMigrationRequired","legacyQuotePermissionCodes","permissionCodes"})
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record RoleDetailResponse(String id,String code,String name,String description,boolean system,long userCount,int permissionCount,long revision,boolean quotePermissionMigrationRequired,java.util.List<String> legacyQuotePermissionCodes,java.util.List<String> permissionCodes) {}
