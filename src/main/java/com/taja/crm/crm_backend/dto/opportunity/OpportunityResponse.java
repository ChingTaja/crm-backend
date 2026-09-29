package com.taja.crm.crm_backend.dto.opportunity;

import com.taja.crm.crm_backend.model.Opportunity;
import java.math.BigDecimal;
import java.time.LocalDate;

public record OpportunityResponse(String id, String name, String customerId, String leadId,
        BigDecimal amount, LocalDate expectedCloseDate, String owner, String stage) {
    public static OpportunityResponse fromEntity(Opportunity value) {
        return new OpportunityResponse(value.getId(), value.getName(), value.getCustomerId(), value.getLeadId(),
                value.getAmount(), value.getExpectedCloseDate(), value.getOwner(), value.getStage());
    }
}
