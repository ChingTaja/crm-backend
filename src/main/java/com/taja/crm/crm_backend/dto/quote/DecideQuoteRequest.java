package com.taja.crm.crm_backend.dto.quote;
import jakarta.validation.constraints.*;
public record DecideQuoteRequest(@NotNull @Positive Long expectedRevision,
        @NotBlank @Pattern(regexp = "accepted|rejected") String decision, @Size(max = 10000) String reason) {}
