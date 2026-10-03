package com.taja.crm.crm_backend.dto.role;
import jakarta.validation.constraints.*;
import java.util.List;
public record UpdateRoleRequest(@NotBlank @Size(max=100) String name,@Size(max=2000) String description,
 @NotNull @Size(max=100) List<@NotBlank String> permissionCodes,@NotNull @Positive Long expectedRevision) {}
