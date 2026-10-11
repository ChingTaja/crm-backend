package com.taja.crm.crm_backend.dto.quote;
import com.taja.crm.crm_backend.model.*;
@io.swagger.v3.oas.annotations.media.Schema(requiredProperties={"id","number","name","customerId","customerName","version","status","approval","totalCents","currency"})
public record QuoteSummaryResponse(String id, String number, String name, String customerId, String customerName,
        String opportunityId, int version, QuoteStatus status, ApprovalStatus approval,
        long totalCents, String currency, String orderId) {
    public static QuoteSummaryResponse of(Quote q, QuoteVersion v) {
        var t = v.getTerms();
        return new QuoteSummaryResponse(q.getId(), q.getNumber(), t.getName(), t.getCustomer().getId(), t.getCustomer().getName(),
                t.getOpportunity() == null ? null : t.getOpportunity().getId(), v.getVersion(), v.getStatus(),
                v.getApproval(), v.getTotals().getTotalCents(), v.getCurrency(), q.getOrderId());
    }
}
