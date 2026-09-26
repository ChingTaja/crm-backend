package com.taja.crm.crm_backend.dto.auth;

import jakarta.validation.constraints.*;

public record ResetPasswordRequest(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String token,
        @NotBlank @Size(min = 8, max = 72) String newPassword) {}
