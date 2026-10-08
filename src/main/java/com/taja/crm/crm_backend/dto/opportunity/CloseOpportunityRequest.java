package com.taja.crm.crm_backend.dto.opportunity;
import jakarta.validation.constraints.*;
public record CloseOpportunityRequest(
        @NotBlank @Pattern(regexp = "won|lost") String outcome,
        @Size(max = 2000) String description) {}
