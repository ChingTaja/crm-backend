package com.taja.crm.crm_backend.dto.auth;

import jakarta.validation.constraints.*;

public record RegisterRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) String password) {}
