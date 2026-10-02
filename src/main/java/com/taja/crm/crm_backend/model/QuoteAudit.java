package com.taja.crm.crm_backend.model;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
@Embeddable @Getter @Setter
public class QuoteAudit {
    @Column(name = "audit_id", nullable = false) private String id = UUID.randomUUID().toString();
    @Column(name = "occurred_at", nullable = false) private Instant at;
    private String actorId;
    private String actorName;
    @Column(nullable = false) private String action;
    private int version;
    @Column(columnDefinition = "text") private String detail;
}
