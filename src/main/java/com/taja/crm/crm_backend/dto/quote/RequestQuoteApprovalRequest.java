package com.taja.crm.crm_backend.dto.quote;
import jakarta.validation.constraints.*;
public record RequestQuoteApprovalRequest(@NotNull @Positive Long expectedRevision, @NotBlank String reviewerId) {}
