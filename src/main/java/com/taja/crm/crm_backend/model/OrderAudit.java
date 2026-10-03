package com.taja.crm.crm_backend.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;
@Embeddable @Getter @Setter
public class OrderAudit {
 private String id = UUID.randomUUID().toString();
 private Instant at;
 private String actorId;
 private String actorName;
 private String action;
 private String fromStatus;
 private String toStatus;
 @Column(columnDefinition="text") private String reason;
}
