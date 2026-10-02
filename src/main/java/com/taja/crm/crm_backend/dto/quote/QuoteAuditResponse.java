package com.taja.crm.crm_backend.dto.quote;
import com.taja.crm.crm_backend.model.QuoteAudit;
import java.time.Instant;
public record QuoteAuditResponse(String id, Instant at, String actorId, String actorName, String action, int version, String detail) {
    public static QuoteAuditResponse of(QuoteAudit a) {
        return new QuoteAuditResponse(a.getId(), a.getAt(), a.getActorId(), a.getActorName(), a.getAction(), a.getVersion(), a.getDetail());
    }
}
