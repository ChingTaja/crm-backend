package com.taja.crm.crm_backend.dto.lead;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record QualifyLeadRequest(
        @NotBlank @Pattern(regexp = "approved|rejected") String decision,
        @Pattern(regexp = "customer_contact|customer_contact_opportunity") String conversionType,
        String reason,
        String note) {}
