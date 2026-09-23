package com.taja.crm.crm_backend.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LeadQualification {

    @NotBlank
    @Pattern(regexp = "approved|rejected")
    @Column(name = "qualification_decision")
    private String decision;

    /** ISO 日期時間字串，例如 2026-09-22T10:00:00+08:00。 */
    @NotBlank
    @Column(name = "qualification_reviewed_at")
    private String reviewedAt;

    @Column(name = "qualification_reason", columnDefinition = "text")
    private String reason;

    @Column(name = "qualification_note", columnDefinition = "text")
    private String note;

    @Column(name = "qualification_customer_id")
    private String customerId;

    @Column(name = "qualification_contact_id")
    private String contactId;

    @Column(name = "qualification_opportunity_id")
    private String opportunityId;
}
