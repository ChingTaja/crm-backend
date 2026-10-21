package com.taja.crm.crm_backend.dto.customer;

import jakarta.validation.constraints.*;

public record CustomerImportRow(
        @NotNull @Positive Integer rowNumber,
        @NotBlank(message = "名稱必填") @Size(max = 255, message = "名稱最多 255 字元") String name,
        @Size(max = 255, message = "公司最多 255 字元") String company,
        @Email(message = "Email 格式不正確") @Size(max = 255, message = "Email 最多 255 字元") String email,
        @Size(max = 255, message = "電話最多 255 字元") String phone,
        @Size(max = 255, message = "負責人最多 255 字元") String owner) {}
