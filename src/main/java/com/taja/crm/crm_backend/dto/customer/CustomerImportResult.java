package com.taja.crm.crm_backend.dto.customer;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(requiredProperties = {"rowNumber", "status", "code", "reason"})
public record CustomerImportResult(
        int rowNumber,
        @Schema(allowableValues = {"success", "skipped", "failed"}) String status,
        String code, String reason, String customerId) {}
