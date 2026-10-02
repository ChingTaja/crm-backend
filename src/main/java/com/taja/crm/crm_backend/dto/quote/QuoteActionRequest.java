package com.taja.crm.crm_backend.dto.quote;
import jakarta.validation.constraints.*;
public record QuoteActionRequest(@NotNull @Positive Long expectedRevision) {}
