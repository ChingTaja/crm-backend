package com.taja.crm.crm_backend.model;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name="refresh_tokens", indexes=@Index(name="ix_refresh_family", columnList="familyId"))
@Getter @Setter
public class RefreshToken {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private String id;
    @Column(nullable=false,unique=true,length=64) private String tokenHash;
    @Column(nullable=false) private String userId;
    @Column(nullable=false) private String familyId;
    @Column(nullable=false) private long tokenVersion;
    @Column(nullable=false) private Instant expiresAt;
    private Instant usedAt;
    @Column(nullable=false) private boolean revoked;
}
