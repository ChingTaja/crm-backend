package com.taja.crm.crm_backend.dto.role;
import jakarta.validation.constraints.*;
import java.util.List;
public record CreateRoleRequest(@NotBlank @Pattern(regexp="[A-Za-z][A-Za-z0-9_]{1,49}") String code,
 @NotBlank @Size(max=100) String name,@Size(max=2000) String description,
 @NotNull @Size(max=100) List<@NotBlank String> permissionCodes) {}
