package com.taja.crm.crm_backend.dto.opportunity;

import com.taja.crm.crm_backend.model.Opportunity;
import java.math.BigDecimal;
import java.time.LocalDate;

public record OpportunityResponse(String id, String name, String customerId, String leadId,
        BigDecimal amount, LocalDate expectedCloseDate, String owner, @io.swagger.v3.oas.annotations.media.Schema(requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED, allowableValues = {"需求討論中", "需求成交", "失單"}) String stage, String closeDescription, java.time.Instant closedAt, String closedByName) {
    public static OpportunityResponse fromEntity(Opportunity value) {
        return new OpportunityResponse(value.getId(), value.getName(), value.getCustomerId(), value.getLeadId(),
                value.getAmount(), value.getExpectedCloseDate(), value.getOwner(), value.getStage(), value.getCloseDescription(), value.getClosedAt(), value.getClosedByName());
    }
}
