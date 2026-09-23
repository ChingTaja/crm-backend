package com.taja.crm.crm_backend.dto.lead;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.taja.crm.crm_backend.model.LeadQualification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LeadQualificationDto(
        @NotBlank @Pattern(regexp = "approved|rejected") String decision,
        @NotBlank String reviewedAt,
        String reason,
        String note,
        String customerId,
        String contactId,
        String opportunityId) {

    public LeadQualification toEntity() {
        LeadQualification qualification = new LeadQualification();
        qualification.setDecision(decision);
        qualification.setReviewedAt(reviewedAt);
        qualification.setReason(reason);
        qualification.setNote(note);
        qualification.setCustomerId(customerId);
        qualification.setContactId(contactId);
        qualification.setOpportunityId(opportunityId);
        return qualification;
    }

    public static LeadQualificationDto fromEntity(LeadQualification qualification) {
        if (qualification == null) {
            return null;
        }
        return new LeadQualificationDto(qualification.getDecision(), qualification.getReviewedAt(),
                qualification.getReason(), qualification.getNote(), qualification.getCustomerId(),
                qualification.getContactId(), qualification.getOpportunityId());
    }
}
