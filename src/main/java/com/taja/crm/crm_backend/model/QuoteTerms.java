package com.taja.crm.crm_backend.model;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Embeddable @Getter @Setter
public class QuoteTerms {
    @Column(nullable = false) private String name;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false) private Customer customer;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id") private Opportunity opportunity;
    @Column(columnDefinition = "text", nullable = false) private String paymentTerms = "";
    @Column(columnDefinition = "text", nullable = false) private String deliveryTerms = "";
    @Column(columnDefinition = "text", nullable = false) private String warranty = "";
    @Column(columnDefinition = "text", nullable = false) private String notes = "";
}
