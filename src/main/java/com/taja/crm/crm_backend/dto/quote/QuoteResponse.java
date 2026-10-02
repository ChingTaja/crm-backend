package com.taja.crm.crm_backend.dto.quote;
import com.taja.crm.crm_backend.model.Quote;
import java.time.LocalDate;
import java.util.List;
public record QuoteResponse(String id, String number, List<QuoteVersionResponse> versions, List<QuoteAuditResponse> audit, String orderId) {
    public static QuoteResponse of(Quote q, LocalDate today) {
        return new QuoteResponse(q.getId(), q.getNumber(), q.getVersions().stream().map(v -> QuoteVersionResponse.of(v, today)).toList(),
                q.getAudit().stream().map(QuoteAuditResponse::of).toList(), q.getOrderId());
    }
}
