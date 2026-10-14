package com.taja.crm.crm_backend.dto.currency;

import jakarta.validation.constraints.*;

public record CreateCurrencyRequest(
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String code,
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String symbol,
        @NotNull @Min(2) @Max(2) Integer fractionDigits,
        @NotNull Boolean active) {}
