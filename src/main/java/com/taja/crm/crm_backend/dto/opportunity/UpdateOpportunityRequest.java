package com.taja.crm.crm_backend.dto.opportunity;

import com.taja.crm.crm_backend.model.Opportunity;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@com.fasterxml.jackson.annotation.JsonIgnoreProperties("stage")
public class UpdateOpportunityRequest {
    @NotBlank @Size(max = 255)
    private String name;
    @NotBlank @Size(max = 255)
    private String customerId;
    @Size(max = 255)
    private String leadId;
    @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2)
    private BigDecimal amount;
    private LocalDate expectedCloseDate;
    @Size(max = 255)
    private String owner;

    public Opportunity toEntity() {
        Opportunity opportunity = new Opportunity();
        opportunity.setName(name);
        opportunity.setCustomerId(customerId);
        opportunity.setLeadId(leadId);
        opportunity.setAmount(amount);
        opportunity.setExpectedCloseDate(expectedCloseDate);
        opportunity.setOwner(owner);
        return opportunity;
    }
}
