package com.taja.crm.crm_backend.dto.quote;
import com.taja.crm.crm_backend.model.Quote;
import java.util.List;
@io.swagger.v3.oas.annotations.media.Schema(requiredProperties={"id","number","versions","audit"})
public record QuoteResponse(String id, String number, List<QuoteVersionResponse> versions, List<QuoteAuditResponse> audit, String orderId) {
    public static QuoteResponse of(Quote q, java.util.function.Function<com.taja.crm.crm_backend.model.QuoteVersion,List<String>> actions) {
        return new QuoteResponse(q.getId(), q.getNumber(), q.getVersions().stream().map(v -> QuoteVersionResponse.of(v, actions.apply(v))).toList(),
                q.getAudit().stream().map(QuoteAuditResponse::of).toList(), q.getOrderId());
    }
}
