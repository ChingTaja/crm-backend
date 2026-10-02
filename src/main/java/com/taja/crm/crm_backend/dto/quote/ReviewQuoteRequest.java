package com.taja.crm.crm_backend.dto.quote;
import jakarta.validation.constraints.*;
public record ReviewQuoteRequest(@NotNull @Positive Long expectedRevision,
        @NotBlank @Pattern(regexp = "approved|rejected") String decision, @Size(max = 10000) String reason) {}
