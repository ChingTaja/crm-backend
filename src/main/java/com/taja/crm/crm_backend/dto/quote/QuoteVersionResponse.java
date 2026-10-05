package com.taja.crm.crm_backend.dto.quote;
import com.taja.crm.crm_backend.model.*;
import java.time.*;
import java.util.List;
@io.swagger.v3.oas.annotations.media.Schema(requiredProperties={"id","version","revision","name","customerId","validUntil","lines","totals","currency","paymentTerms","deliveryTerms","warranty","notes","status","approval","requiresReapproval","createdAt","createdBy","allowedActions"})
public record QuoteVersionResponse(String id, int version, long revision, String name, String customerId, String opportunityId,
        LocalDate validUntil, List<QuoteLineResponse> lines, QuoteTotals totals, String currency,
        String paymentTerms, String deliveryTerms, String warranty, String notes, QuoteStatus status, ApprovalStatus approval,
        boolean requiresReapproval, Instant createdAt, String createdBy, Instant sentAt,
        String approvalBy, Instant approvalAt, String approvalReason, Instant decisionAt, String decisionBy, String decisionReason,
        String reviewerId, String reviewerName, String approvalRequestedBy, Instant approvalRequestedAt,
        List<String> allowedActions) {
    public static QuoteVersionResponse of(QuoteVersion v, LocalDate today, List<String> allowedActions) {
        var t = v.getTerms();
        return new QuoteVersionResponse(v.getId(), v.getVersion(), v.getRevision(), t.getName(), t.getCustomer().getId(),
                t.getOpportunity() == null ? null : t.getOpportunity().getId(), t.getValidUntil(),
                v.getLines().stream().map(QuoteLineResponse::of).toList(), v.getTotals(), v.getCurrency(),
                t.getPaymentTerms(), t.getDeliveryTerms(), t.getWarranty(), t.getNotes(), effectiveStatus(v, today), v.getApproval(),
                v.isRequiresReapproval(), v.getCreatedAt(), v.getCreatedBy(), v.getSentAt(), v.getApprovalBy(), v.getApprovalAt(),
                v.getApprovalReason(), v.getDecisionAt(), v.getDecisionBy(), v.getDecisionReason(), v.getReviewerId(), v.getReviewerName(), v.getApprovalRequestedBy(), v.getApprovalRequestedAt(), allowedActions);
    }
    public static QuoteStatus effectiveStatus(QuoteVersion v, LocalDate today) {
        return v.getStatus() == QuoteStatus.Sent && v.getTerms().getValidUntil().isBefore(today) ? QuoteStatus.Expired : v.getStatus();
    }
}
