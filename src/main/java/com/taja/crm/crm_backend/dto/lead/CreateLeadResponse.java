package com.taja.crm.crm_backend.dto.lead;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.taja.crm.crm_backend.model.Lead;
import com.taja.crm.crm_backend.model.LeadStatus;
import com.taja.crm.crm_backend.model.Option;

public record CreateLeadResponse(
        String id,
        String name,
        String company,
        String email,
        String phone,
        String source,
        String owner,
        Option status,
        @JsonInclude(JsonInclude.Include.NON_NULL) LeadQualificationDto qualification) {

    public static CreateLeadResponse fromEntity(Lead lead) {
        LeadStatus status = LeadStatus.fromValue(lead.getStatus());
        return new CreateLeadResponse(lead.getId(), lead.getName(), lead.getCompany(),
                lead.getEmail(), lead.getPhone(), lead.getSource(), lead.getOwner(),
                new Option(status.getKey(), status.getValue()),
                LeadQualificationDto.fromEntity(lead.getQualification()));
    }
}
