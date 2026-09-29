package com.taja.crm.crm_backend.dto.opportunity;

import com.taja.crm.crm_backend.model.Opportunity;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOpportunityRequest {
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
    @NotBlank @Pattern(regexp = "需求確認|提案報價|協商中|已成交|已失單")
    private String stage = "需求確認";

    public Opportunity toEntity() {
        Opportunity opportunity = new Opportunity();
        opportunity.setName(name);
        opportunity.setCustomerId(customerId);
        opportunity.setLeadId(leadId);
        opportunity.setAmount(amount);
        opportunity.setExpectedCloseDate(expectedCloseDate);
        opportunity.setOwner(owner);
        opportunity.setStage(stage);
        return opportunity;
    }
}
