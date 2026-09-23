package com.taja.crm.crm_backend.dto.lead;

import com.taja.crm.crm_backend.model.Option;
import com.taja.crm.crm_backend.model.Lead;
import com.taja.crm.crm_backend.model.LeadStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateLeadRequest {
    private String name;
    private String company;
    private String email;
    private String phone;
    private String source;
    private String owner;

    @Valid
    private Option status = new Option(
            LeadStatus.PENDING.getKey(),
            LeadStatus.PENDING.getValue()
    );

    @Valid
    private LeadQualificationDto qualification;

    public Lead toEntity() {
        Lead lead = new Lead();
        lead.setName(name);
        lead.setCompany(company);
        lead.setEmail(email);
        lead.setPhone(phone);
        lead.setSource(source);
        lead.setOwner(owner);
        lead.setStatus(
                LeadStatus.fromKey(status.key()).getValue()
        );
        lead.setQualification(qualification == null ? null : qualification.toEntity());
        return lead;
    }
}
