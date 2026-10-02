package com.taja.crm.crm_backend.model;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name = "quotes") @Getter @Setter
public class Quote {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @Column(nullable = false, unique = true) private String number;
    @Column(nullable = false) private String createdBy;
    private String orderId;
    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("version ASC") private List<QuoteVersion> versions = new ArrayList<>();
    @ElementCollection @CollectionTable(name = "quote_audit", joinColumns = @JoinColumn(name = "quote_id"))
    @OrderColumn(name = "audit_index") private List<QuoteAudit> audit = new ArrayList<>();
}
