package com.taja.crm.crm_backend.model;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name = "sales_orders") @Getter @Setter
public class SalesOrder {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false, unique = true) private Quote quote;
    @Column(nullable = false) private String quoteVersionId;
    @Column(nullable = false) private String status = "Confirmed";
    @Column(unique = true) private String number;
    private long revision = 1;
    private String customerName;
    private String quoteNumber;
    private int quoteVersion;
    private Instant updatedAt;
    private Instant processingAt;
    private Instant completedAt;
    private Instant cancelledAt;
    @Column(columnDefinition="text") private String cancellationReason;
    @ElementCollection @CollectionTable(name="sales_order_audit", joinColumns=@JoinColumn(name="order_id"))
    @OrderColumn(name="audit_index") private List<OrderAudit> audit = new ArrayList<>();
    @Column(nullable = false) private String currency = "TWD";
    @Embedded private QuoteTerms terms;
    @Embedded private QuoteTotals totals;
    @ElementCollection @CollectionTable(name = "sales_order_lines", joinColumns = @JoinColumn(name = "order_id"))
    @OrderColumn(name = "line_index") private List<QuoteLine> lines = new ArrayList<>();
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private String createdBy;
}
