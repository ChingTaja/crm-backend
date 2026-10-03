package com.taja.crm.crm_backend.model;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;
@Entity @Table(name="security_audit") @Getter @Setter
public class SecurityAudit {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private String id;
 private Instant at;
 private String actorId;
 private String actorName;
 private String action;
 private String targetId;
 @Column(columnDefinition="text") private String beforeValue;
 @Column(columnDefinition="text") private String afterValue;
}
