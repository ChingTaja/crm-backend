package com.taja.crm.crm_backend.dto.auth;

import jakarta.validation.constraints.*;

public record LoginRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(max = 72) String password) {}
