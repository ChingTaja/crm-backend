package com.taja.crm.crm_backend.dto.user;

import jakarta.validation.constraints.*;

/** 基本資料完整更新；密碼使用既有重設密碼流程。 */
public record UpdateUserRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Email @Size(max = 254) String email,
        String roleId) {}
